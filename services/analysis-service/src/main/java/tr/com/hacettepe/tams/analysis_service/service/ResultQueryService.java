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
 *   <li>STUDENT — can only see results whose studentNumber matches their JWT claim.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResultQueryService {

    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Returns a paginated list of result summaries for a given teacher.
     * Optionally filters by a partial student number substring.
     *
     * @param teacherId     the authenticated teacher's UUID
     * @param studentNumber optional search token (partial student number)
     * @param pageable      pagination and sort parameters
     */
    @Transactional(readOnly = true)
    public Page<AnalysisResultSummaryResponse> listForTeacher(UUID teacherId,
                                                              String studentNumber,
                                                              Pageable pageable) {
        Page<AnalysisResult> page = StringUtils.hasText(studentNumber)
                ? analysisResultRepository.findByTeacherIdAndStudentNumberContaining(
                        teacherId, studentNumber, pageable)
                : analysisResultRepository.findByTeacherId(teacherId, pageable);

        return page.map(result -> {
            result.getGlobalCheckResults().size();
            return AnalysisResultSummaryResponse.from(result);
        });
    }

    /**
     * Returns the full detail of a result by its primary key.
     * A TEACHER may only read results they uploaded; a STUDENT may only read
     * results whose studentNumber matches the supplied value.
     *
     * @param id            the UUID of the analysis result
     * @param role          the caller's role (TEACHER or STUDENT)
     * @param callerId      the authenticated user's UUID (used for TEACHER ownership check)
     * @param studentNumber the caller's student number (used for STUDENT ownership check)
     * @throws ResourceNotFoundException if no result exists with the given id
     * @throws UnauthorizedException     if the caller does not own this result
     */
    @Transactional(readOnly = true)
    public AnalysisResultDetailResponse getById(UUID id,
                                                String role,
                                                UUID callerId,
                                                String studentNumber) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));

        enforceOwnership(result, role, callerId, studentNumber);

        result.getCategoryResults().size();
        result.getTranscriptCourses().size();
        result.getGlobalCheckResults().size();

        return AnalysisResultDetailResponse.from(result);
    }

    /**
     * Returns the full detail of a result by its job ID.
     * Intended for the TEACHER flow: after upload, the teacher polls by jobId.
     *
     * @param jobId the Kafka job ID returned at upload time
     * @throws ResourceNotFoundException if no result exists for the given jobId
     */
    @Transactional(readOnly = true)
    public AnalysisResultDetailResponse getByJobId(String jobId) {
        AnalysisResult result = analysisResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for jobId: " + jobId));

        result.getCategoryResults().size();
        result.getTranscriptCourses().size();
        result.getGlobalCheckResults().size();

        return AnalysisResultDetailResponse.from(result);
    }

    /**
     * Returns the latest completed result for a student identified by their student number.
     *
     * @param studentNumber the student's Öğrenci No from the JWT claim
     * @throws ResourceNotFoundException if no result exists for this student number
     */
    @Transactional(readOnly = true)
    public AnalysisResultDetailResponse getLatestForStudent(String studentNumber) {
        AnalysisResult result = analysisResultRepository
                .findFirstByStudentNumberOrderByCreatedAtDesc(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No results found for the provided student number"));

        result.getCategoryResults().size();
        result.getTranscriptCourses().size();
        result.getGlobalCheckResults().size();

        return AnalysisResultDetailResponse.from(result);
    }

    private void enforceOwnership(AnalysisResult result,
                                  String role,
                                  UUID callerId,
                                  String studentNumber) {
        if ("TEACHER".equals(role)) {
            if (!result.getTeacherId().equals(callerId)) {
                throw new UnauthorizedException("You do not have access to this result");
            }
        } else if ("STUDENT".equals(role)) {
            if (!StringUtils.hasText(studentNumber)
                    || !studentNumber.equals(result.getStudentNumber())) {
                throw new UnauthorizedException("You do not have access to this result");
            }
        } else {
            throw new UnauthorizedException("Unsupported role: " + role);
        }
    }
}
