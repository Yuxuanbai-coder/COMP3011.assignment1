package comp3011.assignment.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment.service.TranscriptionService;

/**
 * Verifies the HTTP boundary of the transcription controller.
 *
 * <p>The service is stubbed so these tests focus on multipart request binding,
 * response serialisation, and the controller's behaviour when many requests
 * overlap.</p>
 */
class TranscriptionControllerTest {

    private TranscriptionService transcriptionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transcriptionService = org.mockito.Mockito.mock(TranscriptionService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TranscriptionController(transcriptionService))
                .build();
    }

    /**
     * Checks that a multipart audio upload is passed to the service and that
     * the returned text is exposed as JSON.
     */
    @Test
    void acceptsAudioUploadAndReturnsTranscription() throws Exception {
        when(transcriptionService.transcribe(any(MultipartFile.class)))
                .thenReturn("hello from the stub service");

        MockMultipartFile audio = new MockMultipartFile(
                "file",
                "recording.webm",
                "audio/webm",
                "audio bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/transcriptions").file(audio))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text", is("hello from the stub service")));

        verify(transcriptionService).transcribe(any(MultipartFile.class));
    }

    /**
     * Starts 220 controller calls together while the stub service deliberately
     * blocks. This checks that requests can overlap without shared controller
     * state corrupting another request's response.
     */
    @Test
    void handlesMoreThanTwoHundredOverlappingControllerRequests() throws Exception {
        int requestCount = 220;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch requestsEnteredService = new CountDownLatch(requestCount);
        CountDownLatch releaseService = new CountDownLatch(1);

        when(transcriptionService.transcribe(any(MultipartFile.class))).thenAnswer(invocation -> {
            requestsEnteredService.countDown();
            try {
                if (!releaseService.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release the stub service");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("The stub service was interrupted", exception);
            }
            return "concurrent response";
        });

        MockMultipartFile audio = new MockMultipartFile(
                "file",
                "recording.webm",
                "audio/webm",
                "audio bytes".getBytes(StandardCharsets.UTF_8));

        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(requestCount);
        List<Future<String>> responses = new ArrayList<>(requestCount);
        try {
            for (int index = 0; index < requestCount; index++) {
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return mockMvc.perform(multipart("/api/v1/transcriptions").file(audio))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(requestsEnteredService.await(10, TimeUnit.SECONDS));
            releaseService.countDown();

            for (Future<String> response : responses) {
                assertEquals("{\"text\":\"concurrent response\"}",
                        response.get(10, TimeUnit.SECONDS));
            }
        } finally {
            releaseService.countDown();
            executor.shutdownNow();
        }

        verify(transcriptionService, times(requestCount))
                .transcribe(any(MultipartFile.class));
    }
}
