package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;

import java.math.BigDecimal;

/**
 * Compact course representation used in the internal rule set response
 * consumed by analysis-service.
 */
public record RuleCourseDto(
        String courseCode,
        String courseName,
        BigDecimal credit,
        BigDecimal ects,
        boolean isMandatory
) {
    public static RuleCourseDto from(CategoryCourse cc) {
        return new RuleCourseDto(
                cc.getCourse().getCourseCode(),
                cc.getCourse().getCourseName(),
                cc.getCourse().getCredit(),
                cc.getCourse().getEcts(),
                cc.isMandatory()
        );
    }
}
