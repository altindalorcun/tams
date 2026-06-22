package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.client.dto.ExemptionRuleDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.PrefixLimitDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCategoryDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCourseDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.service.dto.CategoryEvaluation;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;
import tr.com.hacettepe.tams.analysis_service.service.dto.GlobalCheckResult;
import tr.com.hacettepe.tams.analysis_service.service.dto.GlobalCheckResult.GlobalCheckType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final EnrollmentYearParser enrollmentYearParser;

    /**
     * Runs the graduation eligibility check.
     *
     * @param transcript the PII-free parsed transcript from Kafka
     * @param ruleSet    the full rule set fetched from rule-service
     * @return an {@link EngineResult} with the overall eligibility flag and per-category breakdowns
     */
    public EngineResult evaluate(ParsedTranscriptMessage transcript, RuleSetResponse ruleSet) {
        List<ParsedCourse> allCourses = transcript.allCourses();

        String registrationDate = transcript.metadata() != null
                ? transcript.metadata().registrationDate() : null;
        Integer enrollmentYear = enrollmentYearParser.parse(registrationDate).orElse(null);

        // Index passed courses by code for O(1) lookup during category evaluation.
        Set<String> rawPassedCodes = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> c.courseCode().toUpperCase())
                .collect(Collectors.toSet());

        // Apply exemption rules: if all required codes are passed, add the exempted code.
        Set<String> passedCourseCodes = new HashSet<>(rawPassedCodes);
        List<ExemptionRuleDto> exemptions = ruleSet.exemptionRules() != null
                ? ruleSet.exemptionRules() : List.of();
        for (ExemptionRuleDto rule : exemptions) {
            if (rule.requiredCourseCodes() != null
                    && rule.requiredCourseCodes().stream()
                            .allMatch(c -> passedCourseCodes.contains(c.toUpperCase()))) {
                passedCourseCodes.add(rule.exemptedCourseCode().toUpperCase());
            }
        }

        BigDecimal totalCredit = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> BigDecimal.valueOf(c.credit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEcts = allCourses.stream()
                .filter(ParsedCourse::isPassed)
                .map(c -> BigDecimal.valueOf(c.ects()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gpa = gpaCalculator.calculate(allCourses);

        List<GlobalCheckResult> globalChecks = new ArrayList<>();
        boolean allSatisfied = true;

        if (ruleSet.minTotalEcts() != null && totalEcts.compareTo(ruleSet.minTotalEcts()) < 0) {
            allSatisfied = false;
            globalChecks.add(new GlobalCheckResult(
                    GlobalCheckType.TOTAL_ECTS, false,
                    "Total ECTS " + totalEcts + " is below the required minimum of " + ruleSet.minTotalEcts()));
        } else if (ruleSet.minTotalEcts() != null) {
            globalChecks.add(new GlobalCheckResult(
                    GlobalCheckType.TOTAL_ECTS, true,
                    "Total ECTS " + totalEcts + " meets the required minimum of " + ruleSet.minTotalEcts()));
        }

        if (ruleSet.blockOnAnyFGrade() && allCourses.stream().anyMatch(c -> !c.isPassed())) {
            allSatisfied = false;
            globalChecks.add(new GlobalCheckResult(
                    GlobalCheckType.FAIL_GRADE, false,
                    "Transcript contains one or more failed (F-grade) courses"));
        } else if (ruleSet.blockOnAnyFGrade()) {
            globalChecks.add(new GlobalCheckResult(
                    GlobalCheckType.FAIL_GRADE, true,
                    "No failed courses found in transcript"));
        }

        List<CategoryEvaluation> evaluations = new ArrayList<>();

        for (RuleCategoryDto category : ruleSet.categories()) {
            if (isCohortSkipped(category, enrollmentYear)) {
                evaluations.add(buildSkippedEvaluation(category));
                continue;
            }
            CategoryEvaluation eval = evaluateCategory(category, passedCourseCodes, enrollmentYear);
            evaluations.add(eval);
            if (!eval.satisfied()) {
                allSatisfied = false;
            }
        }

        return new EngineResult(allSatisfied, totalCredit, totalEcts, gpa, evaluations, globalChecks, enrollmentYear);
    }

    /**
     * Returns true when the category's cohort range excludes the student's enrollment year.
     * Null bounds are treated as open (no restriction on that side).
     */
    private boolean isCohortSkipped(RuleCategoryDto category, Integer enrollmentYear) {
        if (enrollmentYear == null) {
            return false;
        }
        if (category.appliesFromYear() != null && enrollmentYear < category.appliesFromYear()) {
            return true;
        }
        if (category.appliesToYear() != null && enrollmentYear > category.appliesToYear()) {
            return true;
        }
        return false;
    }

    /** Builds a placeholder {@link CategoryEvaluation} for a cohort-skipped category. */
    private CategoryEvaluation buildSkippedEvaluation(RuleCategoryDto category) {
        return new CategoryEvaluation(
                category.id(),
                category.name(),
                true,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0,
                List.of(),
                true
        );
    }

    private CategoryEvaluation evaluateCategory(RuleCategoryDto category,
                                                Set<String> passedCourseCodes,
                                                Integer enrollmentYear) {
        List<RuleCourseDto> poolCourses = category.courses() != null
                ? category.courses() : List.of();

        // Determine whether the condition is met: student has passed at least one condition course.
        boolean conditionMet = category.conditionCourseCodes() != null
                && category.conditionCourseCodes().stream()
                        .anyMatch(code -> passedCourseCodes.contains(code.toUpperCase()));

        int effectiveCount = (conditionMet && category.minCourseCountIfMet() != null)
                ? category.minCourseCountIfMet() : category.minCourseCount();

        BigDecimal effectiveEcts = (conditionMet && category.minEctsIfMet() != null)
                ? category.minEctsIfMet() : category.minEcts();

        BigDecimal earnedCredit = BigDecimal.ZERO;
        BigDecimal earnedEcts = BigDecimal.ZERO;
        int earnedCount = 0;
        List<String> missingMandatory = new ArrayList<>();
        List<PrefixLimitDto> prefixLimits = category.prefixLimits() != null
                ? category.prefixLimits() : List.of();

        // Tracks how many passed courses have been counted per prefix.
        Map<String, Integer> prefixCounters = new HashMap<>();

        for (RuleCourseDto poolCourse : poolCourses) {
            String code = poolCourse.courseCode().toUpperCase();
            boolean passed = passedCourseCodes.contains(code);

            // A course is only mandatory when the student's cohort falls within the mandatory range.
            boolean effectivelyMandatory = poolCourse.isMandatory()
                    && isMandatoryForCohort(poolCourse, enrollmentYear);

            if (passed) {
                // If a prefix limit applies and the cap is already reached, skip counting this course.
                String matchedPrefix = findMatchingPrefix(code, prefixLimits);
                if (matchedPrefix != null) {
                    int current = prefixCounters.getOrDefault(matchedPrefix, 0);
                    PrefixLimitDto limit = getPrefixLimit(matchedPrefix, prefixLimits);
                    if (current >= limit.maxCount()) {
                        continue;
                    }
                    prefixCounters.put(matchedPrefix, current + 1);
                }
                earnedCredit = earnedCredit.add(poolCourse.credit() != null
                        ? poolCourse.credit() : BigDecimal.ZERO);
                earnedEcts = earnedEcts.add(poolCourse.ects() != null
                        ? poolCourse.ects() : BigDecimal.ZERO);
                earnedCount++;
            } else if (effectivelyMandatory) {
                missingMandatory.add(poolCourse.courseCode());
            }
        }

        BigDecimal requiredCredit = category.minCredit() != null
                ? category.minCredit() : BigDecimal.ZERO;
        BigDecimal requiredEcts = effectiveEcts != null ? effectiveEcts : BigDecimal.ZERO;
        int requiredCount = effectiveCount;

        // A threshold of zero means "not enforced" — skip the check entirely.
        boolean creditOk = requiredCredit.compareTo(BigDecimal.ZERO) == 0
                || earnedCredit.compareTo(requiredCredit) >= 0;
        boolean ectsOk = requiredEcts.compareTo(BigDecimal.ZERO) == 0
                || earnedEcts.compareTo(requiredEcts) >= 0;
        boolean countOk = requiredCount == 0
                || earnedCount >= requiredCount;
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
                missingMandatory,
                false
        );
    }

    /**
     * Returns the first prefix from the limit list whose value is a prefix of {@code courseCode},
     * or {@code null} if none matches.
     */
    private String findMatchingPrefix(String courseCode, List<PrefixLimitDto> limits) {
        for (PrefixLimitDto limit : limits) {
            if (courseCode.startsWith(limit.courseCodePrefix().toUpperCase())) {
                return limit.courseCodePrefix().toUpperCase();
            }
        }
        return null;
    }

    /** Returns the {@link PrefixLimitDto} whose prefix equals {@code prefix} (case-insensitive). */
    private PrefixLimitDto getPrefixLimit(String prefix, List<PrefixLimitDto> limits) {
        return limits.stream()
                .filter(l -> l.courseCodePrefix().toUpperCase().equals(prefix))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Returns true when a course's mandatory obligation applies to the student's enrollment year.
     * Null bounds mean no restriction on that side.
     */
    private boolean isMandatoryForCohort(RuleCourseDto course, Integer enrollmentYear) {
        if (enrollmentYear == null) {
            return true;
        }
        if (course.mandatoryFromYear() != null && enrollmentYear < course.mandatoryFromYear()) {
            return false;
        }
        if (course.mandatoryToYear() != null && enrollmentYear > course.mandatoryToYear()) {
            return false;
        }
        return true;
    }
}
