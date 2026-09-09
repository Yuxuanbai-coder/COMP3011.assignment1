package comp3011.assignment.api;

import java.time.Instant;

/**
 * Standard error body required by the assignment API specification.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
