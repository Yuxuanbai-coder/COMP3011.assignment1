package comp3011.assignment.service;

/**
 * Signals that the external speech-to-text workflow could not be completed.
 *
 * <p>This application exception separates upstream failures from invalid
 * client input, allowing {@code ApiExceptionHandler} to return HTTP 502
 * without exposing low-level network details.</p>
 */
public class TranscriptionServiceException extends RuntimeException {

    /**
     * Creates an exception with the original upstream cause.
     *
     * @param message safe description for server-side handling
     * @param cause underlying network, parsing, or file failure
     */
    public TranscriptionServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception when there is no separate underlying cause.
     *
     * @param message description of the service failure
     */
    public TranscriptionServiceException(String message) {
        super(message);
    }
}
