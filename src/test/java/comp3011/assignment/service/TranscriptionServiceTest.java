package comp3011.assignment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifies the transcription service at the external HTTP-client boundary.
 *
 * <p>The successful and failed upstream responses are supplied by
 * MockRestServiceServer, so the tests never send an API key or audio to the
 * real OpenAI service.</p>
 */
class TranscriptionServiceTest {

    private static final String TRANSCRIPTION_URL =
            "https://stub.example.test/v1/audio/transcriptions";
    private static final String MODEL = "gpt-4o-mini-transcribe";

    /**
     * Checks request execution, response parsing, and token-statistics update
     * using a stubbed OpenAI response.
     */
    @Test
    void sendsAudioToStubbedOpenAiClientAndRecordsUsage() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();
        RestClient restClient = restClientBuilder.build();
        GlobalStatistics statistics = new GlobalStatistics();

        server.expect(requestTo(TRANSCRIPTION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"text\":\"stub transcription\","
                                + "\"usage\":{\"type\":\"tokens\","
                                + "\"input_tokens\":12,\"output_tokens\":4,"
                                + "\"total_tokens\":16}}",
                        MediaType.APPLICATION_JSON));

        TranscriptionService service = new TranscriptionService(
                restClient, statistics, "test-key", TRANSCRIPTION_URL, MODEL);

        assertEquals("stub transcription", service.transcribe(audioFile()));
        assertEquals(12, statistics.snapshot().inputTokens());
        assertEquals(4, statistics.snapshot().outputTokens());
        server.verify();
    }

    /**
     * Checks that an upstream HTTP 500 is converted into the application's
     * service-specific exception.
     */
    @Test
    void convertsUpstreamServerFailureToTranscriptionServiceException() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();
        RestClient restClient = restClientBuilder.build();

        server.expect(requestTo(TRANSCRIPTION_URL))
                .andRespond(withServerError());

        TranscriptionService service = new TranscriptionService(
                restClient,
                new GlobalStatistics(),
                "test-key",
                TRANSCRIPTION_URL,
                MODEL);

        assertThrows(TranscriptionServiceException.class,
                () -> service.transcribe(audioFile()));
        server.verify();
    }

    /**
     * Checks that a socket timeout is handled in the same safe way as other
     * upstream request failures.
     */
    @Test
    void convertsAnUpstreamTimeoutToTranscriptionServiceException() {
        ClientHttpRequestFactory timeoutFactory = (uri, httpMethod) -> {
            throw new SocketTimeoutException("Read timed out");
        };
        RestClient restClient = RestClient.builder()
                .requestFactory(timeoutFactory)
                .build();
        TranscriptionService service = new TranscriptionService(
                restClient,
                new GlobalStatistics(),
                "test-key",
                TRANSCRIPTION_URL,
                MODEL);

        assertThrows(TranscriptionServiceException.class,
                () -> service.transcribe(audioFile()));
    }

    /**
     * Checks that an explicitly non-audio multipart upload is rejected before
     * any external HTTP request is attempted.
     */
    @Test
    void rejectsAFileThatIsNotAudio() {
        ClientHttpRequestFactory noRequestFactory = (uri, httpMethod) -> {
            throw new AssertionError("The upstream client must not be called for invalid audio");
        };
        RestClient restClient = RestClient.builder()
                .requestFactory(noRequestFactory)
                .build();
        TranscriptionService service = new TranscriptionService(
                restClient,
                new GlobalStatistics(),
                "test-key",
                TRANSCRIPTION_URL,
                MODEL);
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not audio".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> service.transcribe(textFile));
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile(
                "file",
                "recording.webm",
                "audio/webm",
                "audio bytes".getBytes(StandardCharsets.UTF_8));
    }
}
