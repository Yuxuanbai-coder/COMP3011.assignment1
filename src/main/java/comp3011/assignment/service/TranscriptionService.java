package comp3011.assignment.service;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implements the speech-to-text business workflow.
 *
 * <p>The service validates the uploaded file, builds the multipart request for
 * OpenAI, converts the response into Java data, and records token usage. The
 * API key is read from the runtime environment through Spring configuration;
 * it is never sent to the browser or written to logs.</p>
 */
@Service
public class TranscriptionService {

    private final RestClient restClient;
    private final GlobalStatistics globalStatistics;
    private final String apiKey;
    private final String transcriptionUrl;
    private final String transcriptionModel;

    public TranscriptionService(
            RestClient transcriptionRestClient,
            GlobalStatistics globalStatistics,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${openai.transcription-url}") String transcriptionUrl,
            @Value("${openai.transcription-model}") String transcriptionModel) {
        this.restClient = transcriptionRestClient;
        this.globalStatistics = globalStatistics;
        this.apiKey = apiKey;
        this.transcriptionUrl = transcriptionUrl;
        this.transcriptionModel = transcriptionModel;
    }

    /**
     * Sends one uploaded audio file to the configured Cloud transcription API.
     *
     * @param audioFile the browser-uploaded audio file
     * @return the transcription text returned by the Cloud service
     * @throws IllegalArgumentException if the file is empty or explicitly has
     *         a non-audio content type
     * @throws IllegalStateException if the runtime API key is not configured
     * @throws TranscriptionServiceException if the upstream call, response, or
     *         uploaded file cannot be processed
     */
    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("An audio file is required");
        }

        String contentType = audioFile.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new IllegalArgumentException(
                    "The uploaded file must be an audio file");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("model", transcriptionModel);
        requestBody.add("file", asResource(audioFile));

        try {
            OpenAiTranscriptionResponse response = restClient.post()
                    .uri(transcriptionUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiTranscriptionResponse.class);

            if (response == null || response.text() == null) {
                throw new TranscriptionServiceException(
                        "Speech-to-text service returned an empty response");
            }

            if (response.usage() == null) {
                throw new TranscriptionServiceException(
                        "Speech-to-text service returned no token usage");
            }

            globalStatistics.addTokenUsage(
                    response.usage().inputTokens(),
                    response.usage().outputTokens());

            return response.text();
        } catch (TranscriptionServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new TranscriptionServiceException(
                    "Speech-to-text service request failed", exception);
        }
    }

    /**
     * Converts the multipart upload into a resource with a usable filename.
     *
     * <p>Keeping the original filename helps the upstream multipart parser
     * identify the media type. A fallback filename is provided because some
     * clients do not send one.</p>
     *
     * @param audioFile uploaded file to convert
     * @return resource containing the uploaded bytes and filename
     * @throws TranscriptionServiceException if the bytes cannot be read
     */
    private ByteArrayResource asResource(MultipartFile audioFile) {
        try {
            String filename = audioFile.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                filename = "recording.webm";
            }

            final String uploadFilename = filename;
            return new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return uploadFilename;
                }
            };
        } catch (IOException exception) {
            throw new TranscriptionServiceException(
                    "Uploaded audio could not be read", exception);
        }
    }

    /**
     * Response shape returned by the OpenAI transcription endpoint.
     */
    private record OpenAiTranscriptionResponse(
            String text,
            Usage usage) {
    }

    /**
     * Token usage values returned by the transcription API.
     */
    private record Usage(
            @JsonProperty("input_tokens") long inputTokens,
            @JsonProperty("output_tokens") long outputTokens) {
    }
}
