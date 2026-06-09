package tr.com.hacettepe.tams.analysis_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.config.KafkaTopicProperties;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.RawTranscriptMessage;

/**
 * Publishes PDF bytes (Base64-encoded) plus job metadata to the {@code transcript.raw} topic.
 * parser-service consumes from this topic to extract and mask transcript data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptRawProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    /**
     * Serializes the message to JSON and publishes it.
     * The jobId is used as the Kafka message key to ensure ordering per job.
     *
     * @throws IllegalStateException if JSON serialization fails (should never happen in practice)
     */
    public void publish(RawTranscriptMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topicProperties.transcriptRaw(), message.jobId(), payload);
            log.info("Published transcript.raw message for jobId={}", message.jobId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize RawTranscriptMessage for jobId=" + message.jobId(), e);
        }
    }
}
