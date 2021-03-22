package phasza.futurestream.throwing;

/**
 * Represents a function that accepts one argument and produces a result.
 * This function implementation can throw (forward) a given checked exception
 * when used as lambda for example.
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of the checked exception which is thrown by the function
 *
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

    /**
     * Applies this function to the given argument.
     *
     * @param arg the function argument
     * @return the function result
     */
    R apply(T arg) throws E;
}
