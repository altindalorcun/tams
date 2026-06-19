package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCategoryDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCourseDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.service.dto.CategoryEvaluation;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stateless graduation eligibility engine.
 *
 * <p>For each requirement category in the rule set, the engine:
 * <ol>
 *   <li>Intersects the category's course pool with the student's passed courses.</li>
 *   <li>Checks the {@code min_course_count} threshold.</li>
 *   <li>Checks the {@code min_credit} threshold (sum of matched passed course credits).</li>
 *   <li>Checks the {@code min_ects} threshold (sum of matched passed course ECTS).</li>
 *   <li>Verifies that every {@code is_mandatory=true} course was passed, regardless
 *       of whether the count/credit thresholds are already met.</li>
 * </ol>
 *
 * <p>A category is <em>satisfied</em> only when all four checks pass simultaneously.
 * The student is <em>eligible</em> to graduate when every category is satisfied.
 */
@Component
@RequiredArgsConstructor
public class GraduationEngine {

    private final GpaCalculator gpaCalculator;

    /**
     * Runs the graduation eligibility check.
     *
     * @param transcript the PII-free parsed transcript from Kafka
     * @param ruleSet    the full rule set fetched from rule-service
     * @return an {@link EngineResult} with the overall eligibility flag and per-category breakdowns
     */
    public EngineResult evaluate(ParsedTranscriptMessage transcript, RuleSetResponse ruleSet) {
        List<ParsedCourse> allCourses = transcript.allCourses();

        // Index passed courses by code for O(1) lookup during category evaluation.
        Set<String> passedCourseCodes = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> c.courseCode().toUpperCase())
                .collect(Collectors.toSet());

        BigDecimal totalCredit = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> BigDecimal.valueOf(c.credit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEcts = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> BigDecimal.valueOf(c.ects()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gpa = gpaCalculator.calculate(allCourses);

        List<CategoryEvaluation> evaluations = new ArrayList<>();
        boolean allSatisfied = true;

        for (RuleCategoryDto category : ruleSet.categories()) {
            CategoryEvaluation eval = evaluateCategory(category, passedCourseCodes);
            evaluations.add(eval);
            if (!eval.satisfied()) {
                allSatisfied = false;
            }
        }

        return new EngineResult(allSatisfied, totalCredit, totalEcts, gpa, evaluations);
    }

    private CategoryEvaluation evaluateCategory(RuleCategoryDto category,
                                                Set<String> passedCourseCodes) {
        List<RuleCourseDto> poolCourses = category.courses() != null
                ? category.courses() : List.of();

        BigDecimal earnedCredit = BigDecimal.ZERO;
        BigDecimal earnedEcts = BigDecimal.ZERO;
        int earnedCount = 0;
        List<String> missingMandatory = new ArrayList<>();

        for (RuleCourseDto poolCourse : poolCourses) {
            String code = poolCourse.courseCode().toUpperCase();
            boolean passed = passedCourseCodes.contains(code);

            if (passed) {
                earnedCredit = earnedCredit.add(poolCourse.credit() != null
                        ? poolCourse.credit() : BigDecimal.ZERO);
                earnedEcts = earnedEcts.add(poolCourse.ects() != null
                        ? poolCourse.ects() : BigDecimal.ZERO);
                earnedCount++;
            } else if (poolCourse.isMandatory()) {
                missingMandatory.add(poolCourse.courseCode());
            }
        }

        BigDecimal requiredCredit = category.minCredit() != null
                ? category.minCredit() : BigDecimal.ZERO;
        BigDecimal requiredEcts = category.minEcts() != null
                ? category.minEcts() : BigDecimal.ZERO;
        int requiredCount = category.minCourseCount();

        boolean creditOk = earnedCredit.compareTo(requiredCredit) >= 0;
        boolean ectsOk = earnedEcts.compareTo(requiredEcts) >= 0;
        boolean countOk = earnedCount >= requiredCount;
        boolean mandatoryOk = missingMandatory.isEmpty();

        boolean satisfied = creditOk && ectsOk && countOk && mandatoryOk;

        return new CategoryEvaluation(
                category.id(),
                category.name(),
                satisfied,
                requiredCredit,
                earnedCredit,
                requiredEcts,
                earnedEcts,
                requiredCount,
                earnedCount,
                missingMandatory
        );
    }
}
