package comp3011.assignment.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Coordinates the application's one-time graceful shutdown sequence.
 *
 * <p>An {@link AtomicBoolean} prevents two concurrent shutdown requests from
 * starting two shutdown actions. The actual close operation runs on a
 * separate virtual thread so the HTTP response can be sent before the Spring
 * application context is closed.</p>
 */
@Service
public class ShutdownCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownCoordinator.class);

    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final Runnable shutdownAction;

    /**
     * Creates the production coordinator using the Spring application context
     * as the shutdown action.
     *
     * @param applicationContext context that will be closed during shutdown
     */
    @Autowired
    public ShutdownCoordinator(ConfigurableApplicationContext applicationContext) {
        this(() -> applicationContext.close());
    }

    public ShutdownCoordinator(Runnable shutdownAction) {
        this.shutdownAction = shutdownAction;
    }

    /**
     * Accepts the first shutdown request and rejects subsequent requests.
     *
     * @return {@code true} when this call started shutdown, otherwise
     *         {@code false} when shutdown was already requested
     */
    public boolean requestShutdown() {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return false;
        }

        logger.info("Graceful server shutdown requested");
        Thread.startVirtualThread(() -> {
            try {
                // Let the HTTP response leave the server before closing the context.
                Thread.sleep(100);
                shutdownAction.run();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.warn("Graceful shutdown thread was interrupted", exception);
            } catch (RuntimeException exception) {
                logger.error("Graceful shutdown failed", exception);
            }
        });
        return true;
    }
}
