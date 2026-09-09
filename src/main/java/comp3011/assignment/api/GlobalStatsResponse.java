package comp3011.assignment.api;

/**
 * Global token counters since the current server process started.
 */
public record GlobalStatsResponse(long inputTokens, long outputTokens) {
}
