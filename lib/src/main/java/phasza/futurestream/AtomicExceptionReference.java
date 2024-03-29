package phasza.futurestream;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe reference of an exception which is caught during the FutureStream operation.
 * @param <E> The type of the exception which can be stored in the reference.
 */
public class AtomicExceptionReference<E extends Exception> implements ExceptionReference<E> {
    /**
     * Wrapped exception
     */
    private final AtomicReference<E> wrappedException;

    @Override
    public void set(final E exception) {
        wrappedException.set(exception);
    }

    @Override
    public Optional<E> get() {
        return Optional.ofNullable(wrappedException.get());
    }

    /**
     *
     */
    public AtomicExceptionReference() {
        wrappedException = new AtomicReference<>();
    }
}
