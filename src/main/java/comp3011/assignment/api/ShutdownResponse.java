package comp3011.assignment.api;

/**
 * JSON acknowledgement returned when a graceful shutdown request is accepted.
 *
 * @param message human-readable acknowledgement returned to the client
 */
public record ShutdownResponse(String message) {
}
