package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.ExemptionRule;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Read-only representation of an exemption rule returned to API consumers.
 * When a student has passed all {@code requiredCourseCodes}, the engine treats
 * {@code exemptedCourseCode} as passed even if the student hasn't taken it.
 */
public record ExemptionRuleDto(
        UUID id,
        List<String> requiredCourseCodes,
        String exemptedCourseCode
) {
    public static ExemptionRuleDto from(ExemptionRule rule) {
        List<String> codes = rule.getRequiredCourseCodes() != null
                ? Arrays.asList(rule.getRequiredCourseCodes())
                : List.of();
        return new ExemptionRuleDto(rule.getId(), codes, rule.getExemptedCourseCode());
    }
}
