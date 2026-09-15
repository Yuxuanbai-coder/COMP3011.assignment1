package comp3011.assignment.api;

/**
 * JSON response containing cumulative speech-to-text token usage.
 *
 * <p>The counters belong to the current Java process and therefore reset when
 * the server restarts.</p>
 *
 * @param inputTokens total input tokens consumed since server startup
 * @param outputTokens total output tokens produced since server startup
 */
public record GlobalStatsResponse(long inputTokens, long outputTokens) {
}
