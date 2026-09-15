package comp3011.assignment.api;

import java.time.Instant;

/**
 * Standard JSON error body returned when an API operation fails.
 *
 * @param timestamp UTC time at which the error response was created
 * @param status HTTP status code associated with the error
 * @param error short HTTP reason phrase or error category
 * @param message human-readable explanation of the failure
 * @param path request path that produced the error
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
