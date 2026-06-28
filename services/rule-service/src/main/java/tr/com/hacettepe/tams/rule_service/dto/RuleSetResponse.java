package tr.com.hacettepe.tams.rule_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full graduation rule set for a single department.
 * Consumed by analysis-service via {@code GET /internal/rules/{departmentId}}.
 */
public record RuleSetResponse(
        UUID departmentId,
        String departmentName,
        BigDecimal minTotalEcts,
        boolean blockOnAnyFGrade,
        List<RuleCategoryDto> categories,
        List<CurriculumEquivalenceRuleDto> curriculumEquivalenceRules
) {}
