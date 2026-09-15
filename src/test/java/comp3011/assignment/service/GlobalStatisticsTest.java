package comp3011.assignment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the shared token counters remain correct under contention.
 */
class GlobalStatisticsTest {

    /**
     * Performs the same updates from 220 workers and verifies that no
     * increments are lost because of a race condition.
     */
    @Test
    void keepsExactTotalsWhenManyThreadsUpdateCounters() throws Exception {
        int workerCount = 220;
        int updatesPerWorker = 100;
        GlobalStatistics statistics = new GlobalStatistics();
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(workerCount);
        List<Future<?>> workers = new ArrayList<>(workerCount);

        try {
            for (int worker = 0; worker < workerCount; worker++) {
                workers.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    for (int update = 0; update < updatesPerWorker; update++) {
                        statistics.addTokenUsage(3, 2);
                    }
                    return null;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> worker : workers) {
                worker.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals((long) workerCount * updatesPerWorker * 3,
                statistics.snapshot().inputTokens());
        assertEquals((long) workerCount * updatesPerWorker * 2,
                statistics.snapshot().outputTokens());
    }
}
