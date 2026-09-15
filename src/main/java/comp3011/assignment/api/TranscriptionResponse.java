package comp3011.assignment.api;

/**
 * JSON response containing the text returned to the browser after a successful
 * transcription.
 *
 * @param text transcribed speech returned by the Cloud STT service
 */
public record TranscriptionResponse(String text) {
}
