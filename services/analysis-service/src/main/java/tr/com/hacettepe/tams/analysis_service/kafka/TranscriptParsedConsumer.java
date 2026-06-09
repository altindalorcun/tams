package tr.com.hacettepe.tams.analysis_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.client.RuleServiceClient;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.service.GraduationEngine;
import tr.com.hacettepe.tams.analysis_service.service.ResultService;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;

import java.util.UUID;

/**
 * Consumes PII-free parsed transcripts from the {@code transcript.parsed} Kafka topic,
 * fetches the graduation rules for the target department, runs the eligibility engine,
 * and persists the result.
 *
 * <p>Manual acknowledgement is used so the offset is committed only after the result is
 * successfully persisted. If processing fails the offset is still committed to prevent
 * infinite retries on a poison message — instead the job is marked FAILED in the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptParsedConsumer {

    private final ObjectMapper objectMapper;
    private final RuleServiceClient ruleServiceClient;
    private final GraduationEngine graduationEngine;
    private final ResultService resultService;

    @KafkaListener(
            topics = "${app.kafka.topics.transcript-parsed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String jobId = record.key();
        try {
            ParsedTranscriptMessage message = objectMapper.readValue(
                    record.value(), ParsedTranscriptMessage.class);

            log.info("Processing transcript.parsed: jobId={}, studentRef={}",
                    message.jobId(), message.studentRef());

            process(message);
            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Deserialization failed for transcript.parsed message: key={}", jobId, e);
            markFailed(jobId, "Message deserialization failed: " + e.getOriginalMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Unexpected error processing transcript.parsed: key={}", jobId, e);
            markFailed(jobId, "Unexpected processing error: " + e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    private void process(ParsedTranscriptMessage message) {
        String jobId = message.jobId();

        if (message.departmentId() == null) {
            resultService.failResult(jobId, "departmentId missing in parsed message");
            return;
        }

        UUID departmentId;
        try {
            departmentId = UUID.fromString(message.departmentId());
        } catch (IllegalArgumentException e) {
            resultService.failResult(jobId, "Invalid departmentId: " + message.departmentId());
            return;
        }

        RuleSetResponse ruleSet;
        try {
            ruleSet = ruleServiceClient.getRuleSet(departmentId);
        } catch (RuleServiceClient.RuleServiceUnavailableException e) {
            log.error("rule-service unavailable for jobId={}: {}", jobId, e.getMessage());
            resultService.failResult(jobId, "Failed to fetch graduation rules: " + e.getMessage());
            return;
        }

        EngineResult engineResult = graduationEngine.evaluate(message, ruleSet);
        resultService.completeResult(jobId, message, engineResult);
    }

    /**
     * Marks a job FAILED when the jobId is known but something went wrong before
     * we could produce an engine result. Swallows exceptions to avoid re-queuing.
     */
    private void markFailed(String jobId, String reason) {
        if (jobId == null) {
            log.warn("Cannot mark null jobId as failed; reason={}", reason);
            return;
        }
        try {
            resultService.failResult(jobId, reason);
        } catch (Exception ex) {
            log.error("Failed to mark jobId={} as FAILED: {}", jobId, ex.getMessage());
        }
    }
}
