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
 * Administrative and global statistics endpoints from assignment1api.yaml.
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

    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getServerUptime() {
        Instant utcNow = Instant.now();
        double uptimeSeconds = Duration.between(utcServerStart, utcNow).toNanos() / 1_000_000_000.0;
        return new UptimeResponse(utcServerStart, utcNow, Math.max(0.0, uptimeSeconds));
    }

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

    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return globalStatistics.snapshot();
    }
}
