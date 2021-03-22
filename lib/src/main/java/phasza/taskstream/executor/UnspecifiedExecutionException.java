package phasza.taskstream.executor;

/**
 * This exception is thrown when a throwing future stream function catches
 * an exception, which is not specified in the method arguments.
 * The unspecified exception is wrapped in this type and rethrown.
 */
public class UnspecifiedExecutionException extends RuntimeException {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -5991771909856600490L;

    /**
     * New instance of unspecified exception
     * @param message Message
     */
    public UnspecifiedExecutionException(final String message) {
        super(message);
    }

    /**
     * New instance of unspecified exception
     * @param cause Wrapped exception
     */
    public UnspecifiedExecutionException(final Exception cause) {
        super(cause);
    }

    /**
     * New instance of unspecified exception
     * @param message Message
     * @param cause Wrapped exception
     */
    public UnspecifiedExecutionException(final String message, final Exception cause) {
        super(message, cause);
    }
}
