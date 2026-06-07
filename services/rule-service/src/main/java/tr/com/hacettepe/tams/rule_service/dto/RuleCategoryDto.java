package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.math.BigDecimal;
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
        List<RuleCourseDto> courses
) {
    public static RuleCategoryDto from(Category c) {
        List<RuleCourseDto> courses = c.getCategoryCourses().stream()
                .map(RuleCourseDto::from)
                .toList();
        return new RuleCategoryDto(
                c.getId(), c.getName(),
                c.getMinCredit(), c.getMinEcts(), c.getMinCourseCount(),
                courses
        );
    }
}
