package comp3011.assignment.service;

/**
 * Indicates that the external speech-to-text request could not be completed.
 */
public class TranscriptionServiceException extends RuntimeException {

    public TranscriptionServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranscriptionServiceException(String message) {
        super(message);
    }
}
