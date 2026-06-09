package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultDetailResponse;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultSummaryResponse;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResultQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class ResultQueryServiceTest {

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @InjectMocks
    private ResultQueryService resultQueryService;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID otherTeacherId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final String studentRef = "sha256:abc123def456";

    private AnalysisResult completedResult(UUID teacher, String maskedRef) {
        AnalysisResult r = new AnalysisResult();
        r.setJobId(UUID.randomUUID().toString());
        r.setTeacherId(teacher);
        r.setDepartmentId(departmentId);
        r.setMaskedStudentRef(maskedRef);
        r.setStatus(AnalysisStatus.COMPLETED);
        r.setIsEligible(true);
        r.setTotalCredit(BigDecimal.valueOf(120));
        r.setTotalEcts(BigDecimal.valueOf(240));
        return r;
    }

    // ── listForTeacher ────────────────────────────────────────────────────────

    @Test
    void listForTeacher_withNoStudentRef_delegatesToUnfilteredQuery() {
        Page<AnalysisResult> page = new PageImpl<>(List.of(completedResult(teacherId, studentRef)));
        when(analysisResultRepository.findByTeacherId(eq(teacherId), any(Pageable.class)))
                .thenReturn(page);

        Page<AnalysisResultSummaryResponse> result =
                resultQueryService.listForTeacher(teacherId, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(analysisResultRepository).findByTeacherId(eq(teacherId), any(Pageable.class));
    }

    @Test
    void listForTeacher_withStudentRef_delegatesToFilteredQuery() {
        String partial = "abc";
        Page<AnalysisResult> page = new PageImpl<>(List.of(completedResult(teacherId, studentRef)));
        when(analysisResultRepository.findByTeacherIdAndMaskedStudentRefContaining(
                eq(teacherId), eq(partial), any(Pageable.class)))
                .thenReturn(page);

        Page<AnalysisResultSummaryResponse> result =
                resultQueryService.listForTeacher(teacherId, partial, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(analysisResultRepository).findByTeacherIdAndMaskedStudentRefContaining(
                eq(teacherId), eq(partial), any(Pageable.class));
    }

    @Test
    void listForTeacher_blankStudentRef_treatedAsAbsent() {
        Page<AnalysisResult> page = new PageImpl<>(List.of());
        when(analysisResultRepository.findByTeacherId(eq(teacherId), any(Pageable.class)))
                .thenReturn(page);

        resultQueryService.listForTeacher(teacherId, "   ", Pageable.unpaged());

        verify(analysisResultRepository).findByTeacherId(eq(teacherId), any(Pageable.class));
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_forTeacher_withOwnResult_returnsDetail() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        AnalysisResultDetailResponse response =
                resultQueryService.getById(id, "TEACHER", teacherId, null);

        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void getById_forTeacher_withAnotherTeachersResult_throwsUnauthorized() {
        AnalysisResult r = completedResult(otherTeacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resultQueryService.getById(id, "TEACHER", teacherId, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultQueryService.getById(id, "TEACHER", teacherId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_forStudent_withMatchingRef_returnsDetail() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        AnalysisResultDetailResponse response =
                resultQueryService.getById(id, "STUDENT", UUID.randomUUID(), studentRef);

        assertThat(response.maskedStudentRef()).isEqualTo(studentRef);
    }

    @Test
    void getById_forStudent_withNullRef_throwsUnauthorized() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resultQueryService.getById(id, "STUDENT", UUID.randomUUID(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_forStudent_withWrongRef_throwsUnauthorized() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        assertThatThrownBy(() ->
                resultQueryService.getById(id, "STUDENT", UUID.randomUUID(), "sha256:wrongvalue"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_withUnknownRole_throwsUnauthorized() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        UUID id = UUID.randomUUID();
        when(analysisResultRepository.findById(id)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resultQueryService.getById(id, "ADMIN", teacherId, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── getLatestForStudent ───────────────────────────────────────────────────

    @Test
    void getLatestForStudent_whenResultExists_returnsDetail() {
        AnalysisResult r = completedResult(teacherId, studentRef);
        when(analysisResultRepository.findFirstByMaskedStudentRefOrderByCreatedAtDesc(studentRef))
                .thenReturn(Optional.of(r));

        AnalysisResultDetailResponse response = resultQueryService.getLatestForStudent(studentRef);

        assertThat(response.maskedStudentRef()).isEqualTo(studentRef);
        assertThat(response.isEligible()).isTrue();
    }

    @Test
    void getLatestForStudent_whenNoResult_throwsResourceNotFound() {
        when(analysisResultRepository.findFirstByMaskedStudentRefOrderByCreatedAtDesc(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultQueryService.getLatestForStudent("sha256:unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
