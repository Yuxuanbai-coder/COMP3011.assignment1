package comp3011.assignment.api;

/**
 * Acknowledgement returned when a graceful shutdown has been accepted.
 */
public record ShutdownResponse(String message) {
}
