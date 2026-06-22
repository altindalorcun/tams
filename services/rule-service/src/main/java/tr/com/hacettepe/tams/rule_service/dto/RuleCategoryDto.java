package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Category representation used in the internal rule set response
 * consumed by analysis-service.
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
) {
    public static RuleCategoryDto from(Category c) {
        List<RuleCourseDto> courses = c.getCategoryCourses().stream()
                .map(RuleCourseDto::from)
                .toList();
        List<String> conditionCodes = c.getConditionCourseCodes() != null
                ? Arrays.asList(c.getConditionCourseCodes())
                : List.of();
        List<PrefixLimitDto> prefixLimits = c.getPrefixLimits().stream()
                .map(PrefixLimitDto::from)
                .toList();
        return new RuleCategoryDto(
                c.getId(), c.getName(),
                c.getMinCredit(), c.getMinEcts(), c.getMinCourseCount(),
                c.getAppliesFromYear(), c.getAppliesToYear(),
                conditionCodes, c.getMinCourseCountIfMet(), c.getMinEctsIfMet(),
                courses, prefixLimits
        );
    }
}
