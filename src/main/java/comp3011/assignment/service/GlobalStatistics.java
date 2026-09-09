package comp3011.assignment.service;

import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

import comp3011.assignment.api.GlobalStatsResponse;

/**
 * Thread-safe process-wide counters. They intentionally reset when the JVM
 * restarts, as required by the assignment specification.
 */
@Service
public class GlobalStatistics {

    private final LongAdder inputTokens = new LongAdder();
    private final LongAdder outputTokens = new LongAdder();

    public void addTokenUsage(long inputTokenCount, long outputTokenCount) {
        if (inputTokenCount < 0 || outputTokenCount < 0) {
            throw new IllegalArgumentException("Token counts cannot be negative");
        }
        inputTokens.add(inputTokenCount);
        outputTokens.add(outputTokenCount);
    }

    public GlobalStatsResponse snapshot() {
        return new GlobalStatsResponse(inputTokens.sum(), outputTokens.sum());
    }
}
