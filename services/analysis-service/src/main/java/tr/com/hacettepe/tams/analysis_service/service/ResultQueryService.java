package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultDetailResponse;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultSummaryResponse;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.util.UUID;

/**
 * Handles read-only queries for analysis results with role-aware ownership checks.
 *
 * <p>Access rules:
 * <ul>
 *   <li>TEACHER — can only see results they personally uploaded (teacherId matches).</li>
 *   <li>STUDENT — can only see results whose maskedStudentRef matches the value they
 *       supply. The masked ref is a deterministic SHA-256 hash computed by parser-service
 *       from the student's raw TC + Öğrenci No; it is the student's persistent identity
 *       token within this system.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResultQueryService {

    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Returns a paginated list of result summaries for a given teacher.
     * Optionally filters by a partial maskedStudentRef substring.
     *
     * @param teacherId    the authenticated teacher's UUID
     * @param studentRef   optional search token (partial maskedStudentRef)
     * @param pageable     pagination and sort parameters
     */
    @Transactional(readOnly = true)
    public Page<AnalysisResultSummaryResponse> listForTeacher(UUID teacherId,
                                                              String studentRef,
                                                              Pageable pageable) {
        Page<AnalysisResult> page = StringUtils.hasText(studentRef)
                ? analysisResultRepository.findByTeacherIdAndMaskedStudentRefContaining(
                        teacherId, studentRef, pageable)
                : analysisResultRepository.findByTeacherId(teacherId, pageable);

        return page.map(AnalysisResultSummaryResponse::from);
    }

    /**
     * Returns the full detail of a result by its primary key.
     * A TEACHER may only read results they uploaded; a STUDENT may only read
     * results whose maskedStudentRef matches the supplied token.
     *
     * @param id           the UUID of the analysis result
     * @param role         the caller's role (TEACHER or STUDENT)
     * @param callerId     the authenticated user's UUID (used for TEACHER ownership check)
     * @param studentRef   the caller's masked student ref (used for STUDENT ownership check)
     * @throws ResourceNotFoundException if no result exists with the given id
     * @throws UnauthorizedException     if the caller does not own this result
     */
    @Transactional(readOnly = true)
    public AnalysisResultDetailResponse getById(UUID id,
                                                String role,
                                                UUID callerId,
                                                String studentRef) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));

        enforceOwnership(result, role, callerId, studentRef);

        // Trigger lazy-load of collections inside the transaction boundary.
        result.getDeficiencies().size();
        result.getTranscriptCourses().size();

        return AnalysisResultDetailResponse.from(result);
    }

    /**
     * Returns the latest completed result for a student identified by their masked ref.
     *
     * @param studentRef the student's deterministic masked identity token
     * @throws ResourceNotFoundException if no result exists for this student ref
     */
    @Transactional(readOnly = true)
    public AnalysisResultDetailResponse getLatestForStudent(String studentRef) {
        AnalysisResult result = analysisResultRepository
                .findFirstByMaskedStudentRefOrderByCreatedAtDesc(studentRef)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No results found for the provided student reference"));

        result.getDeficiencies().size();
        result.getTranscriptCourses().size();

        return AnalysisResultDetailResponse.from(result);
    }

    private void enforceOwnership(AnalysisResult result,
                                  String role,
                                  UUID callerId,
                                  String studentRef) {
        if ("TEACHER".equals(role)) {
            if (!result.getTeacherId().equals(callerId)) {
                throw new UnauthorizedException("You do not have access to this result");
            }
        } else if ("STUDENT".equals(role)) {
            if (!StringUtils.hasText(studentRef)
                    || !studentRef.equals(result.getMaskedStudentRef())) {
                throw new UnauthorizedException("You do not have access to this result");
            }
        } else {
            throw new UnauthorizedException("Unsupported role: " + role);
        }
    }
}
