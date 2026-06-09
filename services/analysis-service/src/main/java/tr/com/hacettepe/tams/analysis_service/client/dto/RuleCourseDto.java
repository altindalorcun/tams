package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.math.BigDecimal;

/**
 * Course entry within a rule category, as returned by
 * {@code GET /internal/rules/{departmentId}} on rule-service.
 */
public record RuleCourseDto(
        String courseCode,
        String courseName,
        BigDecimal credit,
        BigDecimal ects,
        boolean isMandatory
) {}
