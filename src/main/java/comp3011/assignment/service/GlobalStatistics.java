package comp3011.assignment.service;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

import comp3011.assignment.api.GlobalStatsResponse;

/**
 * Thread-safe process-wide token counters.
 *
 * <p>{@link LongAdder} is used because many transcription requests may update
 * the counters concurrently. The values intentionally reset when the JVM
 * restarts, matching the assignment requirement that statistics cover the
 * current server process only.</p>
 */
@Service
public class GlobalStatistics {

    private final LongAdder inputTokens = new LongAdder();
    private final LongAdder outputTokens = new LongAdder();

    /**
     * Adds one transcription request's token usage to the cumulative totals.
     *
     * @param inputTokenCount number of input tokens used by the request
     * @param outputTokenCount number of output tokens produced by the request
     * @throws IllegalArgumentException if either count is negative
     */
    public void addTokenUsage(long inputTokenCount, long outputTokenCount) {
        if (inputTokenCount < 0 || outputTokenCount < 0) {
            throw new IllegalArgumentException("Token counts cannot be negative");
        }
        inputTokens.add(inputTokenCount);
        outputTokens.add(outputTokenCount);
    }

    /**
     * Reads a point-in-time view of the current counters.
     *
     * @return a response DTO containing the cumulative totals
     */
    public GlobalStatsResponse snapshot() {
        return new GlobalStatsResponse(inputTokens.sum(), outputTokens.sum());
    }
}
