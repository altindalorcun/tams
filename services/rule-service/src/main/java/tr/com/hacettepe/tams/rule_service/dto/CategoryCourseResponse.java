package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;

import java.math.BigDecimal;
import java.util.UUID;

/** Response body representing a course assignment within a category. */
public record CategoryCourseResponse(
        UUID courseId,
        String courseCode,
        String courseName,
        BigDecimal credit,
        BigDecimal ects,
        boolean isMandatory
) {
    public static CategoryCourseResponse from(CategoryCourse cc) {
        return new CategoryCourseResponse(
                cc.getCourse().getId(),
                cc.getCourse().getCourseCode(),
                cc.getCourse().getCourseName(),
                cc.getCourse().getCredit(),
                cc.getCourse().getEcts(),
                cc.isMandatory()
        );
    }
}
