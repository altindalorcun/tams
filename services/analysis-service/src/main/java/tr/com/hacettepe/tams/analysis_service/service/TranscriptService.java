package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.dto.UploadResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.RawTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.kafka.TranscriptRawProducer;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.util.Base64;
import java.util.UUID;

/**
 * Handles transcript upload logic: persists a PENDING result, then publishes
 * the PDF bytes to Kafka for async parsing and analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final AnalysisResultRepository analysisResultRepository;
    private final TranscriptRawProducer transcriptRawProducer;

    /**
     * Creates a PENDING analysis result, publishes the PDF to Kafka, and returns the jobId.
     *
     * @param pdfBytes      raw PDF bytes from the multipart upload
     * @param departmentId  UUID of the department whose rules should be applied
     * @param teacherId     UUID of the authenticated teacher making the request
     * @return an {@link UploadResponse} containing the jobId and initial status
     */
    @Transactional
    public UploadResponse uploadTranscript(byte[] pdfBytes, UUID departmentId, UUID teacherId) {
        String jobId = UUID.randomUUID().toString();

        AnalysisResult result = new AnalysisResult();
        result.setJobId(jobId);
        result.setTeacherId(teacherId);
        result.setDepartmentId(departmentId);
        result.setStatus(AnalysisStatus.PENDING);
        analysisResultRepository.save(result);

        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        RawTranscriptMessage message = new RawTranscriptMessage(
                jobId,
                teacherId.toString(),
                departmentId.toString(),
                pdfBase64
        );
        transcriptRawProducer.publish(message);

        log.info("Transcript upload accepted: jobId={}, teacherId={}, departmentId={}",
                jobId, teacherId, departmentId);

        return new UploadResponse(jobId, AnalysisStatus.PENDING.name());
    }

    /**
     * Returns the current status of a job by jobId.
     *
     * @throws ResourceNotFoundException if no job with the given id exists
     */
    @Transactional(readOnly = true)
    public AnalysisStatus getJobStatus(String jobId) {
        return analysisResultRepository.findByJobId(jobId)
                .map(AnalysisResult::getStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }
}
