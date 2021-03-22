package phasza.taskstream.executor;

import java.util.Optional;

/**
 * Common interface of the exception references used to propagate exceptions
 * during the parallel execution of the {@link FutureStream}
 * @param <E> Type of the handled exception
 */
public interface ExceptionReference<E extends Exception> {

    /**
     * Sets the exception for this reference
     * @param exception Exception to set
     */
    void set(E exception);

    /**
     * @return Returns the wrapped exception or {@link Optional#empty()} if not present
     */
    Optional<E> get();
}
