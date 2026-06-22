package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Graduation requirement category, as returned by
 * {@code GET /internal/rules/{departmentId}} on rule-service.
 *
 * <p>Conditional threshold fields: when any course in {@code conditionCourseCodes}
 * is present in the student's passed courses, the engine substitutes
 * {@code minCourseCountIfMet} / {@code minEctsIfMet} for the base thresholds
 * (only when those fields are non-null).
 *
 * <p>Prefix limits: each entry in {@code prefixLimits} caps how many courses
 * sharing that code prefix can count towards the category thresholds.
 */
public record RuleCategoryDto(
        UUID id,
        String name,
        BigDecimal minCredit,
        BigDecimal minEcts,
        int minCourseCount,
        Integer appliesFromYear,
        Integer appliesToYear,
        List<String> conditionCourseCodes,
        Integer minCourseCountIfMet,
        BigDecimal minEctsIfMet,
        List<RuleCourseDto> courses,
        List<PrefixLimitDto> prefixLimits
) {}
