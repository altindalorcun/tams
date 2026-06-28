package tr.com.hacettepe.tams.analysis_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.client.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands the set of courses a student is considered to have passed by applying
 * curriculum equivalence rules in a fixpoint loop.
 *
 * <p>The engine iterates all rules repeatedly until no new codes are added in a full
 * pass, which guarantees termination (the code set is finite and grows monotonically)
 * and handles rule chains where one equivalence can enable another.
 *
 * <p>The original {@code rawPassedCodes} map is never mutated; only the returned
 * set is extended.
 */
@Component
@RequiredArgsConstructor
public class CurriculumEquivalenceExpander {

    private final AcademicYearParser academicYearParser;

    /**
     * Builds an expanded set of passed course codes by applying all equivalence rules.
     *
     * @param rawPassedCodes  mapping from uppercase course code to the raw {@link ParsedCourse}
     *                        (used to retrieve the Başarı Yılı for GROUP legacy checks)
     * @param rules           the list of equivalence rules from the rule-set
     * @return a new mutable set containing all original codes plus any inferred codes
     */
    public Set<String> expand(Map<String, ParsedCourse> rawPassedCodes, List<CurriculumEquivalenceRuleDto> rules) {
        Set<String> passed = new HashSet<>(rawPassedCodes.keySet());
        if (rules == null || rules.isEmpty()) {
            return passed;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (CurriculumEquivalenceRuleDto rule : rules) {
                int before = passed.size();
                applyRule(rule, rawPassedCodes, passed);
                if (passed.size() > before) {
                    changed = true;
                }
            }
        }
        return passed;
    }

    private void applyRule(CurriculumEquivalenceRuleDto rule,
                           Map<String, ParsedCourse> rawPassedCodes,
                           Set<String> passed) {
        switch (rule.ruleType()) {
            case "PAIRWISE" -> applyPairwise(rule, passed);
            case "GROUP_LEGACY_TO_REPLACEMENT" -> applyLegacyToReplacement(rule, rawPassedCodes, passed);
            case "GROUP_REPLACEMENT_TO_LEGACY" -> applyReplacementToLegacy(rule, passed);
            case "GROUP_MUTUAL" -> {
                applyLegacyToReplacement(rule, rawPassedCodes, passed);
                applyReplacementToLegacy(rule, passed);
            }
            default -> { /* unknown type — skip silently */ }
        }
    }

    /**
     * PAIRWISE: each legacy[i] ↔ replacement[i] is individually bi-directional.
     * Effective date is deliberately ignored; HAS222 taken in 18-19 and MUH104 taken
     * in 20-21 are both valid regardless of when the curriculum change occurred.
     */
    private void applyPairwise(CurriculumEquivalenceRuleDto rule, Set<String> passed) {
        List<String> legacy = upperList(rule.legacyCourseCodes());
        List<String> replacement = upperList(rule.replacementCourseCodes());
        int size = Math.min(legacy.size(), replacement.size());
        for (int i = 0; i < size; i++) {
            String leg = legacy.get(i);
            String rep = replacement.get(i);
            if (passed.contains(leg)) passed.add(rep);
            if (passed.contains(rep)) passed.add(leg);
        }
    }

    /**
     * GROUP_LEGACY_TO_REPLACEMENT: if all legacy courses were passed before the effective date,
     * all replacement codes are added to the passed set.
     * When no effective year is configured, the date check is skipped.
     */
    private void applyLegacyToReplacement(CurriculumEquivalenceRuleDto rule,
                                           Map<String, ParsedCourse> rawPassedCodes,
                                           Set<String> passed) {
        List<String> legacy = upperList(rule.legacyCourseCodes());
        if (legacy.isEmpty()) return;

        boolean allLegacyPassed = legacy.stream().allMatch(code -> {
            if (!passed.contains(code)) return false;
            if (rule.effectiveFromYear() == null) return true;
            ParsedCourse course = rawPassedCodes.get(code);
            if (course == null) return true; // was added synthetically by another rule — count it
            return academicYearParser.isBeforeEffective(
                    course.academicYear(), rule.effectiveFromYear(), rule.effectiveFromTerm());
        });

        if (allLegacyPassed) {
            upperList(rule.replacementCourseCodes()).forEach(passed::add);
        }
    }

    /**
     * GROUP_REPLACEMENT_TO_LEGACY: if all replacement courses are passed (regardless of date),
     * all legacy codes are added to the passed set.
     */
    private void applyReplacementToLegacy(CurriculumEquivalenceRuleDto rule, Set<String> passed) {
        List<String> replacement = upperList(rule.replacementCourseCodes());
        if (replacement.isEmpty()) return;

        if (replacement.stream().allMatch(passed::contains)) {
            upperList(rule.legacyCourseCodes()).forEach(passed::add);
        }
    }

    private List<String> upperList(List<String> codes) {
        if (codes == null) return List.of();
        return codes.stream().map(String::toUpperCase).toList();
    }
}
