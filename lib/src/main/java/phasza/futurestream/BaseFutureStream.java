package phasza.futurestream;

import phasza.futurestream.throwing.ThrowingFunction;
import phasza.futurestream.throwing.ThrowingConsumer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An extension methods around the {@link Stream} to add additional parallel functionality.
 * <p>There are two missing capabilities from Java Streams:
 * <ul>
 *  <li>Parallel streams use the common ForkJoinPool, so the actor cannot control the executor service
 *  nor the number of threads to use (programmatically)</li>
 *  <li>Java functional lambdas cannot forward checked exceptions, which means that they must be handled inside
 *  the functional method. Also exceptions are local to a thread, so if we write a functional, parallel stream,
 *  the handling (and debugging) of checked exceptions becomes hard.</li>
 * </ul>
 *
 * <p>BaseFutureStream and the implementing {@link FutureStream} provides the functionality to add these
 * capabilities, with limitation. The intention is not to reimplement the Stream pipeline but to provide
 * the minimal functionality needed.
 *
 * @param <T> the type of the stream elements
 * @see Stream
 */
public interface BaseFutureStream<T> extends AutoCloseable {


    /**
     * Returns the stream wrapped in the BaseFutureStream
     * @return Stream wrapped in by the BaseFutureStream
     */
    Stream<T> boxed();

    /**
     * Returns a BaseFutureStream consisting of the results of applying the given
     * function to the elements of this stream parallel. The parallel threads are
     * joined after this operation.
     * <p>In code it is equivalent to:
     * <pre>{@code
     * return BaseFutureSteam.of(boxed()
     *      .map(i -> CompletableFuture.supplyAsync(() -> mapper.apply(i)))
     *      .map(CompletableFuture::join))
     * }</pre>
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     *  operation</a>. Which means that the evaluation will only occur when a
     *  terminal operation is called on the stream.
     *
     * @param <R> The element type of the new stream
     * @param mapper a function to apply to each element
     * @return the new stream
     */
    <R> BaseFutureStream<R> mapJoining(Function<? super T, R> mapper);

    /**
     * The difference between this and the {@link #mapJoining(Function)} is that
     * this variant is able to retry the mapping operation in case of an error (e.g. HTTP connection).
     * Unlike the {@link #forEachParallel(ThrowingConsumer, Class)} method, this method
     * cannot propagate the exception to the caller as it is lazy evaluated.
     *
     * <p></p>Returns a BaseFutureStream consisting of the results of applying the given
     * function to the elements of this stream parallel. The parallel threads are
     * joined after this operation.
     * <p>In code it is equivalent to:
     * <pre>{@code
     * return BaseFutureSteam.of(boxed()
     *      .map(i -> CompletableFuture.supplyAsync(() -> mapper.apply(i)))
     *      .map(CompletableFuture::join))
     * }</pre>
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     *  operation</a>. Which means that the evaluation will only occur when a
     *  terminal operation is called on the stream.
     *
     * @param <R> The element type of the new stream
     * @param <E> Exception thrown by the mapping function
     * @param mapper a function to apply to each element, which may throw a given exception
     * @return the new stream
     */
    <R, E extends Exception> BaseFutureStream<R> mapJoining(
            ThrowingFunction<? super T, R, ? extends E> mapper,
            Class<E> exType);

    /**
     * Performs an action for each element of this stream parallel on the
     * executor underneath.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>. Which means that this function will evaluate the stream.
     *
     * @param action an action to perform on the elements
     */
    void forEachParallel(Consumer<? super T> action);

    /**
     * The difference between this and the {@link #forEachParallel(Consumer)} is that
     * this variant is able to propagate the given Exception type to the caller.
     *
     * <p></p>Performs an action for each element of this stream parallel.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>. Which means that this function will evaluate the stream.
     *
     * @param action an action to perform on the elements
     */
    <E extends Exception> void forEachParallel(
            ThrowingConsumer<? super T, ? extends E> action,
            Class<E> exType) throws E;
}
