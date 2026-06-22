package tr.com.hacettepe.tams.analysis_service.service.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete output of the graduation eligibility engine for one student transcript.
 *
 * @param eligible            true when every category in the rule set is fully satisfied and all global checks pass
 * @param totalCredit         sum of credits across all passed courses in the transcript
 * @param totalEcts           sum of ECTS across all passed courses in the transcript
 * @param gpa                 cumulative GPA computed using the Hacettepe 4.00 grading scale
 * @param categoryEvaluations per-category breakdown; includes both satisfied and deficient categories
 * @param globalChecks        results of department-level global rules (ECTS threshold, fail-grade block)
 * @param enrollmentYear      calendar year parsed from the transcript's registration date; null if absent
 */
public record EngineResult(
        boolean eligible,
        BigDecimal totalCredit,
        BigDecimal totalEcts,
        BigDecimal gpa,
        List<CategoryEvaluation> categoryEvaluations,
        List<GlobalCheckResult> globalChecks,
        Integer enrollmentYear
) {}
