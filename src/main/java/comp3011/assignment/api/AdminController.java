package comp3011.assignment.api;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment.service.GlobalStatistics;
import comp3011.assignment.service.ShutdownCoordinator;

/**
 * Exposes the administrative and global statistics endpoints required by the
 * assignment API specification.
 *
 * <p>This controller does not contain the implementation of statistics or
 * shutdown behaviour itself. It delegates those responsibilities to
 * {@link GlobalStatistics} and {@link ShutdownCoordinator}, keeping the HTTP
 * layer focused on mapping requests to responses and status codes.</p>
 */
@RestController
public class AdminController {

    private static final String SHUTDOWN_PATH = "/api/v1/admin/shutdown";

    private final Instant utcServerStart = Instant.now();
    private final GlobalStatistics globalStatistics;
    private final ShutdownCoordinator shutdownCoordinator;

    public AdminController(GlobalStatistics globalStatistics, ShutdownCoordinator shutdownCoordinator) {
        this.globalStatistics = globalStatistics;
        this.shutdownCoordinator = shutdownCoordinator;
    }

    /**
     * Returns the UTC server start time, the current UTC time, and the elapsed
     * uptime in seconds.
     *
     * @return a response object that is automatically serialised as JSON
     */
    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getServerUptime() {
        Instant utcNow = Instant.now();
        double uptimeSeconds = Duration.between(utcServerStart, utcNow).toNanos() / 1_000_000_000.0;
        return new UptimeResponse(utcServerStart, utcNow, Math.max(0.0, uptimeSeconds));
    }

    /**
     * Requests a graceful server shutdown.
     *
     * <p>The coordinator accepts the first request and rejects later requests
     * while shutdown is already in progress. This method therefore returns
     * HTTP 202 for an accepted request and HTTP 409 for a duplicate request.</p>
     *
     * @return the HTTP response describing whether shutdown was accepted
     */
    @PostMapping(SHUTDOWN_PATH)
    public ResponseEntity<?> shutdownServer() {
        if (shutdownCoordinator.requestShutdown()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new ShutdownResponse("Graceful shutdown requested."));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        "Graceful shutdown is already in progress.",
                        SHUTDOWN_PATH));
    }

    /**
     * Returns the process-wide token counters collected since server startup.
     *
     * @return the current cumulative input and output token totals
     */
    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return globalStatistics.snapshot();
    }
}
