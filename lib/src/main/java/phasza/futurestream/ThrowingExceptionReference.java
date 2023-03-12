package phasza.futurestream;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An exception reference implementation, which automatically re-throws an exception
 * @param <E> The type of the exception which can be stored in the reference.
 */
public class ThrowingExceptionReference<E extends Exception> implements ExceptionReference<E> {
    /**
     * Wrapped exception
     */
    private final AtomicReference<E> wrappedException = new AtomicReference<>();

    @Override
    public void set(final E exception) throws E {
        wrappedException.set(exception);
        throw exception;
    }

    @Override
    public Optional<E> get() {
        return Optional.ofNullable(wrappedException.get());
    }
}
