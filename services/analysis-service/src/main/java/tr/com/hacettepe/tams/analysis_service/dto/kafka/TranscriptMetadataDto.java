package tr.com.hacettepe.tams.analysis_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional metadata section of the parsed transcript Kafka message.
 * Fields may be absent when the parser-service could not extract them from the PDF.
 */
public record TranscriptMetadataDto(
        @JsonProperty("registration_date") String registrationDate
) {}
