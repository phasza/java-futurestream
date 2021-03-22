package phasza.futurestream.demo.exception;

/**
 * This is an unchecked exception which gets thrown when a command runs into a runtime exception that results in
 * the failure of said command. This exception should be caught and logged on the top of the call chain.
 */
public final class OperationException extends RuntimeException {

    private static final long serialVersionUID = 5711867540077045300L;

    /**
     * @param message Exception message
     */
    public OperationException(final String message) {
        super(message);
    }

    /**
     * @param message Exception message
     * @param cause Exception cause
     */
    public OperationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
