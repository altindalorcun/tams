package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Full graduation rule set for a single department, as returned by
 * {@code GET /internal/rules/{departmentId}} on rule-service.
 */
public record RuleSetResponse(
        UUID departmentId,
        String departmentName,
        List<RuleCategoryDto> categories
) {}
