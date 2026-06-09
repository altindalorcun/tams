package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.dto.UploadResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.RawTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.kafka.TranscriptRawProducer;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TranscriptService}.
 */
@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Mock
    private TranscriptRawProducer transcriptRawProducer;

    @InjectMocks
    private TranscriptService transcriptService;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final byte[] pdf = "fake-pdf-bytes".getBytes();

    @Test
    void uploadTranscript_persistsResultWithPendingStatus() {
        when(analysisResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transcriptService.uploadTranscript(pdf, departmentId, teacherId);

        ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void uploadTranscript_persistsResultWithCorrectTeacherAndDepartment() {
        when(analysisResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transcriptService.uploadTranscript(pdf, departmentId, teacherId);

        ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacherId()).isEqualTo(teacherId);
        assertThat(captor.getValue().getDepartmentId()).isEqualTo(departmentId);
    }

    @Test
    void uploadTranscript_publishesMessageWithPdfBase64ToKafka() {
        when(analysisResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transcriptService.uploadTranscript(pdf, departmentId, teacherId);

        ArgumentCaptor<RawTranscriptMessage> captor = ArgumentCaptor.forClass(RawTranscriptMessage.class);
        verify(transcriptRawProducer).publish(captor.capture());
        RawTranscriptMessage msg = captor.getValue();
        assertThat(msg.teacherId()).isEqualTo(teacherId.toString());
        assertThat(msg.departmentId()).isEqualTo(departmentId.toString());
        assertThat(msg.pdfBase64()).isNotBlank();
        assertThat(msg.jobId()).isNotBlank();
    }

    @Test
    void uploadTranscript_returnsJobIdMatchingPersistedRecord() {
        when(analysisResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UploadResponse response = transcriptService.uploadTranscript(pdf, departmentId, teacherId);

        ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).save(captor.capture());
        assertThat(response.jobId()).isEqualTo(captor.getValue().getJobId());
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void getJobStatus_returnsCorrectStatus_whenJobExists() {
        AnalysisResult result = new AnalysisResult();
        result.setJobId("job-123");
        result.setStatus(AnalysisStatus.COMPLETED);
        when(analysisResultRepository.findByJobId("job-123")).thenReturn(Optional.of(result));

        AnalysisStatus status = transcriptService.getJobStatus("job-123");

        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
    }

    @Test
    void getJobStatus_throwsResourceNotFoundException_whenJobDoesNotExist() {
        when(analysisResultRepository.findByJobId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transcriptService.getJobStatus("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
