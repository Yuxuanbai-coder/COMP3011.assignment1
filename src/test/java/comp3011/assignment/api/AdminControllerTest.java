package comp3011.assignment.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import comp3011.assignment.service.GlobalStatistics;
import comp3011.assignment.service.ShutdownCoordinator;

/**
 * Verifies the HTTP contract of the administrative controller without
 * starting a real network server.
 *
 * <p>The tests use MockMvc and a no-op shutdown action so the response status,
 * JSON fields, and duplicate-shutdown behaviour can be checked safely.</p>
 */
class AdminControllerTest {

    private GlobalStatistics globalStatistics;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        globalStatistics = new GlobalStatistics();
        ShutdownCoordinator shutdownCoordinator = new ShutdownCoordinator(() -> {
        });
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(globalStatistics, shutdownCoordinator))
                .build();
    }

    /**
     * Checks that the uptime endpoint returns both timestamps and a
     * non-negative elapsed time.
     */
    @Test
    void returnsUptimeInformation() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").exists())
                .andExpect(jsonPath("$.utcNow").exists())
                .andExpect(jsonPath("$.serverUptimeSeconds", greaterThanOrEqualTo(0.0)));
    }

    /**
     * Checks that the statistics endpoint exposes the values held by the
     * process-wide statistics service.
     */
    @Test
    void returnsAccumulatedGlobalStatistics() throws Exception {
        globalStatistics.addTokenUsage(18432, 4096);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens", is(18432)))
                .andExpect(jsonPath("$.outputTokens", is(4096)));
    }

    /**
     * Checks that the first shutdown request is accepted and a second request
     * is rejected with the required conflict response.
     */
    @Test
    void acceptsOnlyTheFirstShutdownRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", is("Graceful shutdown requested.")));

        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.path", is("/api/v1/admin/shutdown")));
    }
}
