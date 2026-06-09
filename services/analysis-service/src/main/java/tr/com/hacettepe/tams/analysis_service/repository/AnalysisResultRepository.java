package tr.com.hacettepe.tams.analysis_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link AnalysisResult} entities.
 */
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    Optional<AnalysisResult> findByJobId(String jobId);

    Page<AnalysisResult> findByTeacherId(UUID teacherId, Pageable pageable);

    Page<AnalysisResult> findByTeacherIdAndMaskedStudentRefContaining(
            UUID teacherId, String maskedStudentRef, Pageable pageable);

    Optional<AnalysisResult> findFirstByMaskedStudentRefOrderByCreatedAtDesc(String maskedStudentRef);
}
