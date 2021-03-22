package phasza.futurestream.throwing;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result.
 * This consumer implementation can throw (forward) a given checked exception
 * when used as lambda for example.
 *
 * @param <T> the type of the input to the operation
 * @param <E> the type of the checked exception thrown by the operation
 *
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    /**
     * Performs this operation on the given argument.
     *
     * @param arg the input argument
     */
    void accept(T arg) throws E;
}
