package comp3011.assignment.api;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import comp3011.assignment.service.TranscriptionServiceException;

/**
 * Converts exceptions raised by controllers and services into the common JSON
 * error format required by the API specification.
 *
 * <p>Centralising this mapping means individual controllers can focus on their
 * successful responses. It also prevents internal exception messages and
 * credentials from being returned to browser clients.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Handles invalid client input, such as an empty or unsupported audio
     * upload.
     *
     * @param exception the validation failure
     * @param request the HTTP request that contained the invalid input
     * @return a HTTP 400 response in the standard error format
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles failures returned by the external speech-to-text service.
     *
     * <p>The detailed exception is logged for server-side diagnosis, while the
     * client receives a generic message that does not reveal implementation
     * details or the API key.</p>
     *
     * @param exception the wrapped upstream failure
     * @param request the HTTP request that triggered the upstream call
     * @return a HTTP 502 response in the standard error format
     */
    @ExceptionHandler(TranscriptionServiceException.class)
    public ResponseEntity<ErrorResponse> handleTranscriptionException(
            TranscriptionServiceException exception,
            HttpServletRequest request) {
        logger.error("Speech-to-text request failed for {} {}", request.getMethod(), request.getRequestURI(), exception);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                "The speech-to-text service could not process the audio.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    /**
     * Provides a final safety net for exceptions not handled more specifically.
     *
     * @param exception the unexpected failure
     * @param request the HTTP request being processed
     * @return a HTTP 500 response without exposing the exception details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        logger.error("Unhandled API error for {} {}", request.getMethod(), request.getRequestURI(), exception);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected server error occurred.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
