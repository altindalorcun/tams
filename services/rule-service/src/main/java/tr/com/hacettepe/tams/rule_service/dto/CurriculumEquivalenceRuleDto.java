package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.CurriculumEquivalenceRule;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Read-only representation of a curriculum equivalence rule returned to API consumers.
 * The {@code ruleType} field controls how the graduation engine applies the equivalence.
 */
public record CurriculumEquivalenceRuleDto(
        UUID id,
        String ruleType,
        List<String> legacyCourseCodes,
        List<String> replacementCourseCodes,
        Integer effectiveFromYear,
        String effectiveFromTerm
) {
    public static CurriculumEquivalenceRuleDto from(CurriculumEquivalenceRule rule) {
        return new CurriculumEquivalenceRuleDto(
                rule.getId(),
                rule.getRuleType(),
                rule.getLegacyCourseCodes() != null ? Arrays.asList(rule.getLegacyCourseCodes()) : List.of(),
                rule.getReplacementCourseCodes() != null ? Arrays.asList(rule.getReplacementCourseCodes()) : List.of(),
                rule.getEffectiveFromYear(),
                rule.getEffectiveFromTerm()
        );
    }
}
