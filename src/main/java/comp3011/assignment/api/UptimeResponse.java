package comp3011.assignment.api;

import java.time.Instant;

/**
 * The server lifecycle information required by assignment1api.yaml.
 */
public record UptimeResponse(
        Instant utcServerStart,
        Instant utcNow,
        double serverUptimeSeconds) {
}
