package tr.com.hacettepe.tams.analysis_service.service.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete output of the graduation eligibility engine for one student transcript.
 *
 * @param eligible            true when every category in the rule set is fully satisfied
 * @param totalCredit         sum of credits across all passed courses in the transcript
 * @param totalEcts           sum of ECTS across all passed courses in the transcript
 * @param gpa                 cumulative GPA computed using the Hacettepe 4.00 grading scale
 * @param categoryEvaluations per-category breakdown; includes both satisfied and deficient categories
 */
public record EngineResult(
        boolean eligible,
        BigDecimal totalCredit,
        BigDecimal totalEcts,
        BigDecimal gpa,
        List<CategoryEvaluation> categoryEvaluations
) {}
