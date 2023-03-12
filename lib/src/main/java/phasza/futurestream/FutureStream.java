package phasza.futurestream;

import phasza.futurestream.throwing.ThrowingFunction;
import phasza.futurestream.throwing.ThrowingConsumer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Implementation of the {@link BaseFutureStream} interface.
 * This class wraps a given stream providing additional functions:
 * <ul>
 *     <li>{@link #mapJoining(Function)} and {@link #forEachParallel(Consumer)}
 *     to apply functions on the stream using the custom {@link ExecutorService}
 *     </li>
 *     <li>Propagating a given ExceptionType to the caller by the throwing functional
 *     interface of the functions above.</li>
 *     <li>The ability to retry a failure in case the given exception type occurs
 *     (e.g. parallel download with retrying an HTTP error response</li>
 * </ul>
 *
 * <p>The class provides a similar fluent API to the Java {@link Stream}. For example:
 * <pre>{@code
 *  Stream<String> sites;
 *  try (FutureStream<String> sitesToPing = FutureStream.of(sites.filter(i -> i.contains("google")))) {
 *      return sitesToPing.mapJoining(i -> pingSite(i))
 *      .boxed()
 *      .filter(i -> i.isSuccessful())
 *      .count()
 *  }
 * }</pre>
 *
 * <p>FutureStream can be created by either {@link #of(Stream)} factory, which simply wraps
 * the given stream in the future stream, initializes a new fixedThreadPool {@link ExecutorService}
 * with the number of processors available.
 * <p>!!! In this usage the future stream must be wrapped in a try-resource to terminate the built-in executor. !!!
 * <p> Or using the {@link FutureStreamBuilder} which enables of configuring an external executor.
 * In this case the caller is responsible for closing the executor or by providing the {@link #shutdownExecutor}
 * the FutureStream will close it when the AutoClosable close method is called.
 * @param <T> the type of the stream elements
 */
public class FutureStream<T> implements BaseFutureStream<T> {

    /**
     * Executor service which used by the {@link CompletableFuture} operations.
     * <p> If not registered via the {@link FutureStreamBuilder} then a default
     * fixedThreadPool is created with the number of available processors as thread.
     *ß
     * <p> Setting the {@link #shutdownExecutor} to True, will mean that the class will
     * terminate the executor then the {@link #close()} method is called, e.g. in a try-resource.
     */
    private final ExecutorService executor;

    /**
     * If applying the given function throws at {@link #mapJoining(ThrowingFunction, Class)}
     * or {@link #forEachParallel(ThrowingConsumer, Class)}, the same operation is retried again the
     * number of times registered in this field.
     * <p>This retry is local to a single application, so each single item is retried a maximum of retries times.
     * <p>0 or less means no retries</p>
     */
    private final int retries;

    /**
     * If set true the {@link #close()} method will terminate the executor referenced by this class.
     * If not set, the user is responsible for terminating the executor.
     */
    private final boolean shutdownExecutor;

    /**
     * The stream wrapped by this instance.
     */
    private final Stream<T> boxed;

    /**
     * Creates a new future stream
     * @param executor Executor service which handles the parallel jobs
     * @param boxed Stream wrapped with future stream
     * @param retries Number of retries for each element. If 0 or less automatically terminates.
     * @param shutdownExecutor If true the auto-close of this class will close the executor service also
     */
    public FutureStream(
            final ExecutorService executor,
            final Stream<T> boxed,
            final int retries,
            final boolean shutdownExecutor) {
        this.executor = executor;
        this.boxed = boxed;
        this.retries = retries;
        this.shutdownExecutor = shutdownExecutor;
    }

    /**
     *
     */
    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns a builder instance to control the properties of FutureStream
     * @param stream Stream to wrap in the FutureStream
     * @param <R> The type of elements in the stream
     * @return A new builder
     */
    public static <R> FutureStreamBuilder<R> builder(final Stream<R> stream) {
        return new FutureStreamBuilder<>(stream);
    }

    /**
     * Simple creator of FutureStream with
     * <ul>
     *     <li>default executor of {@link Executors#newFixedThreadPool(int)}
     *     with {@link Runtime#availableProcessors()} threads</li>
     *     <li>0 retries (no retries)</li>
     *     <li>shutting down the executor {@link #close()}</li>
     * </ul>
     * Always wrap this FutureStream in a try-resource, or close it manually otherwise
     * the executor service won't be terminated.
     * @param stream Stream to wrap
     * @param <R> the type of elements in the stream
     * @return new FutureStream
     */
    public static <R> FutureStream<R> of(final Stream<R> stream) {
        return FutureStream.builder(stream)
                .shutdownExecutor(true)
                .build();
    }

    private static <R, T> FutureStream<R> copy(
            final FutureStream<T> other,
            final Stream<R> newStream) {
        return new FutureStream<>(other.executor, newStream, other.retries, other.shutdownExecutor);
    }

    @Override
    public Stream<T> boxed() {
        return boxed;
    }

    @Override
    public <R> FutureStream<R> mapJoining(final Function<? super T, R> mapper) {
        return copy(
                this,
                boxed.map(i -> CompletableFuture.supplyAsync(() -> mapper.apply(i), executor))
                        .map(FutureStream::completableJoin));
    }

    @Override
    public <R, E extends Exception> FutureStream<R> mapJoining(
            final ThrowingFunction<? super T, R, ? extends E> mapper,
            final Class<E> exType) {
        return copy(
                this,
                boxed.map(i -> CompletableFuture.supplyAsync(
                        () -> applyWithRetry(mapper, i, new ThrowingExceptionReference<>(), exType), executor))
                        .map(FutureStream::completableJoin)
                        .filter(Optional::isPresent)
                        .map(Optional::get));
    }

    @Override
    public void forEachParallel(final Consumer<? super T> action) {
        boxed.map(i -> CompletableFuture.runAsync(
                () -> action.accept(i), executor))
                .map(FutureStream::completableJoin)
                .collect(Collectors.toSet());
    }

    @Override
    public <E extends Exception> void forEachParallel(
            final ThrowingConsumer<? super T, ? extends E> action,
            final Class<E> exType) throws E {
        run((exRef) -> boxed.map(i -> CompletableFuture.runAsync(
                () -> acceptWithRetry(action, i, exRef, exType), executor))
                .map(FutureStream::completableJoin)
                .collect(Collectors.toSet()));
    }

    private <R, E extends Exception> R run(
            final Function<ExceptionReference<Exception>, ? extends R> mapper) throws E {
        final ExceptionReference<Exception> exception = new AtomicExceptionReference<>();
        final R result = mapper.apply(exception); //NOPMD - exception is a side effect of this
        if (exception.get().isPresent()) {
            throw (E) exception.get().get();
        }
        return result;
    }

    private <E extends Exception> void acceptWithRetry(
            final ThrowingConsumer<? super T, ? extends E> action,
            final T arg,
            final ExceptionReference<Exception> exRef,
            final Class<E> exType) {
        applyWithRetry(arg1 -> {
            action.accept(arg);
            return null;
        }, arg, exRef, exType);
    }

    private <R, E extends Exception> Optional<R> applyWithRetry(
            final ThrowingFunction<? super T, ? extends R, ? extends E> mapper,
            final T arg,
            final ExceptionReference<Exception> exRef,
            final Class<? extends E> exType) {
        //An exception present means an exception occurred earlier, so we kill the rest
        if (exRef.get().isPresent()) {
            return Optional.empty();
        }

        int retried = retries; //NOPMD - not a real DU
        while (true) {
            try {
                return Optional.ofNullable(mapper.apply(arg));
            } catch (Exception e) { //NOPMD - we must catch a generic exception here
                if (!exType.isInstance(e)) {
                    throw new UncheckedExecutionException(e);
                }

                if (retried <= 0) {
                    try {
                        exRef.set(exType.cast(e));
                    } catch (Exception setEx) { //NOPMD - must catch a generic exception
                        //Forward the exception if it was thrown by a throwing reference
                        throw new UncheckedExecutionException(setEx.getMessage(), setEx); //NOPMD - new exception
                    }
                    return Optional.empty();
                }

                retried -= 1;
            }
        }
    }

    private static <R> R completableJoin(final CompletableFuture<R> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    @Override
    public void close() throws InterruptedException {
        boxed.close();
        if (shutdownExecutor) {
            executor.shutdownNow();
            executor.awaitTermination(100L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Builder class to construct a {@link FutureStream}
     * @param <T> Type of stream elements
     */
    public static class FutureStreamBuilder<T> {

        /**
         * Boxed stream
         */
        private final Stream<T> stream;

        /**
         * Executor service which runs the parallel jobs
         */
        private ExecutorService executor;

        /**
         * Number of retries per element, after which the operation is terminated
         */
        private int retries;

        /**
         * If true the {@link FutureStream} close method will close the boxed executor service as well
         */
        private boolean shutdownExecutor;

        /**
         * Executor service which runs the parallel jobs.
         * If not provided then a default fixed thread pool executor is used with the available CPU count
         */
        public FutureStreamBuilder<T> executor(final ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Number of retries per element, after which the operation is terminated.
         *      numRetries <= 0 -> automatically terminates
         */
        public FutureStreamBuilder<T> retries(final int retries) {
            this.retries = retries;
            return this;
        }

        /**
         * If true the {@link FutureStream} close method will close the boxed executor service as well.
         * If set to false you are responsible for closing the executor!
         */
        public FutureStreamBuilder<T> shutdownExecutor(final boolean shutdownExecutor) {
            this.shutdownExecutor = shutdownExecutor;
            return this;
        }

        /**
         * @return Future stream from the builder properties
         */
        public FutureStream<T> build() {
            return new FutureStream<>(
                    Optional.ofNullable(executor).orElseGet(
                            () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())),
                    stream,
                    retries,
                    shutdownExecutor);
        }

        /**
         * @param stream Boxed stream
         */
        protected FutureStreamBuilder(final Stream<T> stream) {
            this.stream = stream;
        }

    }
}
