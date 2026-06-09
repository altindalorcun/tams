package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Graduation requirement category, as returned by
 * {@code GET /internal/rules/{departmentId}} on rule-service.
 */
public record RuleCategoryDto(
        UUID id,
        String name,
        BigDecimal minCredit,
        BigDecimal minEcts,
        int minCourseCount,
        List<RuleCourseDto> courses
) {}
