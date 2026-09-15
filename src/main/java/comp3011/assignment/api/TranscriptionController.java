package comp3011.assignment.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment.service.TranscriptionService;

/**
 * HTTP adapter for the browser audio transcription endpoint.
 *
 * <p>The controller receives a multipart file and delegates the actual file
 * validation and Cloud API interaction to {@link TranscriptionService}. The
 * returned text is wrapped in {@link TranscriptionResponse} so Spring can
 * serialise it as the required JSON response.</p>
 */
@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    /**
     * Receives an uploaded audio file and returns its transcription.
     *
     * @param file audio supplied in the multipart field named {@code file}
     * @return the transcribed text in a JSON response
     */
    @PostMapping(
            value = {"/api/v1/transcriptions", "/api/v1/transcribe"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TranscriptionResponse transcribe(@RequestPart("file") MultipartFile file) {
        return new TranscriptionResponse(transcriptionService.transcribe(file));
    }
}
