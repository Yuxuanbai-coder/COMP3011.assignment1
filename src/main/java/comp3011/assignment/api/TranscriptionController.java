package comp3011.assignment.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment.service.TranscriptionService;

/**
 * Receives browser audio and returns the corresponding transcription.
 */
@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(
            value = {"/api/v1/transcriptions", "/api/v1/transcribe"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TranscriptionResponse transcribe(@RequestPart("file") MultipartFile file) {
        return new TranscriptionResponse(transcriptionService.transcribe(file));
    }
}
