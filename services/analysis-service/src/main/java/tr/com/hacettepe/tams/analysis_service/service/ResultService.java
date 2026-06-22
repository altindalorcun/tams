package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.TranscriptMetadataDto;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.domain.CategoryResult;
import tr.com.hacettepe.tams.analysis_service.domain.TranscriptCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedSemester;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;
import tr.com.hacettepe.tams.analysis_service.service.dto.CategoryEvaluation;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;
import tr.com.hacettepe.tams.analysis_service.service.dto.GlobalCheckResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Persists the graduation engine output to the database.
 * Updates the existing PENDING {@link AnalysisResult} row and creates child
 * {@link CategoryResult} and {@link TranscriptCourse} rows in a single transaction.
 *
 * <p>All categories (both satisfied and unsatisfied) are stored as {@link CategoryResult}
 * rows, enabling per-category progress display in the frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final AnalysisResultRepository analysisResultRepository;
    private final EnrollmentYearParser enrollmentYearParser;

    /**
     * Transitions a PENDING result to COMPLETED: stores department name, GPA, all
     * category evaluations, and the transcript course snapshot, then marks the result
     * eligible or not.
     *
     * @param jobId     the jobId used to locate the existing PENDING row
     * @param parsed    the PII-free parsed transcript from Kafka
     * @param engineOut the output of the graduation engine
     * @param ruleSet   the rule set used for evaluation (provides departmentName)
     * @throws ResourceNotFoundException if no result row exists for the given jobId
     */
    @Transactional
    public void completeResult(String jobId,
                               ParsedTranscriptMessage parsed,
                               EngineResult engineOut,
                               RuleSetResponse ruleSet) {
        AnalysisResult result = analysisResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found for jobId=" + jobId));

        result.setMaskedStudentRef(maskStudentNumber(parsed.studentRef()));
        result.setIsEligible(engineOut.eligible());
        result.setTotalCredit(engineOut.totalCredit());
        result.setTotalEcts(engineOut.totalEcts());
        result.setGpa(engineOut.gpa());
        result.setDepartmentName(ruleSet.departmentName());
        result.setStatus(AnalysisStatus.COMPLETED);
        result.setCompletedAt(OffsetDateTime.now());

        applyEnrollmentFields(result, parsed.metadata());
        attachCategoryResults(result, engineOut.categoryEvaluations());
        attachTranscriptCourses(result, parsed);
        applyGlobalCheckSummary(result, engineOut.globalChecks());

        analysisResultRepository.save(result);
        log.info("Analysis completed: jobId={}, eligible={}, gpa={}", jobId, engineOut.eligible(), engineOut.gpa());
    }

    /**
     * Marks a job as FAILED and records the reason without overwriting existing data.
     *
     * @param jobId        the jobId of the failed job
     * @param errorMessage a short description of what went wrong
     */
    @Transactional
    public void failResult(String jobId, String errorMessage) {
        analysisResultRepository.findByJobId(jobId).ifPresentOrElse(result -> {
            result.setStatus(AnalysisStatus.FAILED);
            result.setErrorMessage(errorMessage);
            result.setCompletedAt(OffsetDateTime.now());
            analysisResultRepository.save(result);
            log.warn("Analysis failed: jobId={}, reason={}", jobId, errorMessage);
        }, () -> log.error("Cannot fail unknown jobId={}", jobId));
    }

    /**
     * Masks a student identifier by replacing all but the last four characters with asterisks.
     * Used both when persisting the result and when performing JWT-based student lookup.
     *
     * @param studentNumber raw student number or already-masked identifier
     * @return masked identifier, e.g. {@code "****0001"} for {@code "20190001"}
     */
    public static String maskStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.length() <= 4) {
            return studentNumber;
        }
        return "*".repeat(studentNumber.length() - 4) + studentNumber.substring(studentNumber.length() - 4);
    }

    /**
     * Populates {@code enrollmentYear} and {@code enrollmentTerm} on the result entity
     * using the registration date from the transcript metadata.
     * Silently skips when metadata or registration date is absent.
     */
    private void applyEnrollmentFields(AnalysisResult result, TranscriptMetadataDto metadata) {
        if (metadata == null || metadata.registrationDate() == null) {
            return;
        }
        enrollmentYearParser.parse(metadata.registrationDate())
                .ifPresent(result::setEnrollmentYear);
        result.setEnrollmentTerm(enrollmentYearParser.parseTerm(metadata.registrationDate()));
    }

    private void attachCategoryResults(AnalysisResult result, List<CategoryEvaluation> evaluations) {
        for (CategoryEvaluation eval : evaluations) {
            CategoryResult cr = new CategoryResult();
            cr.setResult(result);
            cr.setCategoryId(eval.categoryId());
            cr.setCategoryName(eval.categoryName());
            cr.setSatisfied(eval.satisfied());
            cr.setRequiredCredit(eval.requiredCredit());
            cr.setEarnedCredit(eval.earnedCredit());
            cr.setRequiredEcts(eval.requiredEcts());
            cr.setEarnedEcts(eval.earnedEcts());
            cr.setRequiredCourseCount(eval.requiredCourseCount());
            cr.setEarnedCourseCount(eval.earnedCourseCount());
            cr.setMissingMandatoryCourses(eval.missingMandatoryCourses().toArray(String[]::new));
            cr.setCohortSkipped(eval.cohortSkipped());
            result.getCategoryResults().add(cr);
        }
    }

    /**
     * Writes a summary of any failed global checks (ECTS threshold, fail-grade block) into the
     * result's errorMessage field so the frontend can display the reasons for ineligibility.
     * Only adds content when at least one global check failed; prefixes entries with
     * {@code [GLOBAL_CHECK]} to distinguish them from technical failure messages.
     */
    private void applyGlobalCheckSummary(AnalysisResult result, List<GlobalCheckResult> globalChecks) {
        if (globalChecks == null || globalChecks.isEmpty()) {
            return;
        }
        List<String> failedDetails = globalChecks.stream()
                .filter(gc -> !gc.passed())
                .map(gc -> "[" + gc.checkType().name() + "] " + gc.detail())
                .toList();
        if (!failedDetails.isEmpty()) {
            result.setErrorMessage(String.join("; ", failedDetails));
        }
    }

    private void attachTranscriptCourses(AnalysisResult result, ParsedTranscriptMessage parsed) {
        if (parsed.semesters() == null) {
            return;
        }
        for (ParsedSemester semester : parsed.semesters()) {
            if (semester.courses() == null) {
                continue;
            }
            for (ParsedCourse course : semester.courses()) {
                TranscriptCourse tc = new TranscriptCourse();
                tc.setResult(result);
                tc.setCourseCode(course.courseCode());
                tc.setCourseName(course.courseName());
                tc.setCredit(BigDecimal.valueOf(course.credit()));
                tc.setEcts(BigDecimal.valueOf(course.ects()));
                tc.setGrade(course.grade());
                tc.setSemester(semester.name());
                tc.setPassed(course.isPassed());
                result.getTranscriptCourses().add(tc);
            }
        }
    }
}
