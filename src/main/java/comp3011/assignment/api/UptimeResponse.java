package comp3011.assignment.api;

import java.time.Instant;

/**
 * JSON response describing the current server lifecycle information.
 *
 * @param utcServerStart UTC timestamp captured when the server controller was
 *        created
 * @param utcNow UTC timestamp captured while creating the response
 * @param serverUptimeSeconds elapsed time between the two timestamps
 */
public record UptimeResponse(
        Instant utcServerStart,
        Instant utcNow,
        double serverUptimeSeconds) {
}
