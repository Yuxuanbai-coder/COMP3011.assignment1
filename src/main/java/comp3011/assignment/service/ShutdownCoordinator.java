package comp3011.assignment.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Ensures that only one graceful shutdown sequence can be requested.
 */
@Service
public class ShutdownCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownCoordinator.class);

    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final Runnable shutdownAction;

    @Autowired
    public ShutdownCoordinator(ConfigurableApplicationContext applicationContext) {
        this(() -> applicationContext.close());
    }

    public ShutdownCoordinator(Runnable shutdownAction) {
        this.shutdownAction = shutdownAction;
    }

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
