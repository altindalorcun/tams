package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.domain.Deficiency;
import tr.com.hacettepe.tams.analysis_service.domain.TranscriptCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedSemester;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;
import tr.com.hacettepe.tams.analysis_service.service.dto.CategoryEvaluation;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Persists the graduation engine output to the database.
 * Updates the existing PENDING {@link AnalysisResult} row and creates
 * child {@link Deficiency} and {@link TranscriptCourse} rows in a single transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Transitions a PENDING result to COMPLETED: attaches all deficiencies,
     * persists a course snapshot, and marks the result eligible or not.
     *
     * @param jobId      the jobId used to locate the existing PENDING row
     * @param parsed     the PII-free parsed transcript from Kafka
     * @param engineOut  the output of the graduation engine
     * @throws ResourceNotFoundException if no result row exists for the given jobId
     */
    @Transactional
    public void completeResult(String jobId,
                               ParsedTranscriptMessage parsed,
                               EngineResult engineOut) {
        AnalysisResult result = analysisResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found for jobId=" + jobId));

        result.setMaskedStudentRef(parsed.studentRef());
        result.setIsEligible(engineOut.eligible());
        result.setTotalCredit(engineOut.totalCredit());
        result.setTotalEcts(engineOut.totalEcts());
        result.setStatus(AnalysisStatus.COMPLETED);
        result.setCompletedAt(OffsetDateTime.now());

        attachDeficiencies(result, engineOut.categoryEvaluations());
        attachTranscriptCourses(result, parsed);

        analysisResultRepository.save(result);
        log.info("Analysis completed: jobId={}, eligible={}", jobId, engineOut.eligible());
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

    private void attachDeficiencies(AnalysisResult result,
                                    List<CategoryEvaluation> evaluations) {
        for (CategoryEvaluation eval : evaluations) {
            if (eval.satisfied()) {
                continue;
            }
            Deficiency d = new Deficiency();
            d.setResult(result);
            d.setCategoryName(eval.categoryName());
            d.setRequiredCredit(eval.requiredCredit());
            d.setEarnedCredit(eval.earnedCredit());
            d.setRequiredEcts(eval.requiredEcts());
            d.setEarnedEcts(eval.earnedEcts());
            d.setMissingCourses(eval.missingMandatoryCourses().toArray(String[]::new));
            result.getDeficiencies().add(d);
        }
    }

    private void attachTranscriptCourses(AnalysisResult result,
                                         ParsedTranscriptMessage parsed) {
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
