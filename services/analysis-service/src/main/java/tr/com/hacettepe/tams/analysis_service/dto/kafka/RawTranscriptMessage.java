package tr.com.hacettepe.tams.analysis_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message published to the {@code transcript.raw} Kafka topic.
 * The PDF is Base64-encoded so it survives JSON serialization.
 * Mirrors the {@code RawTranscriptMessage} model in parser-service.
 */
public record RawTranscriptMessage(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("teacher_id") String teacherId,
        @JsonProperty("department_id") String departmentId,
        @JsonProperty("pdf_base64") String pdfBase64
) {}
