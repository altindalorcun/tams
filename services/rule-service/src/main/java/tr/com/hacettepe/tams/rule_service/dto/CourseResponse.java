package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Course;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Response body representing a course from the global catalog. */
public record CourseResponse(
        UUID id,
        String courseCode,
        String courseName,
        BigDecimal credit,
        BigDecimal ects,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<UUID> departmentIds
) {
    public static CourseResponse from(Course c) {
        return from(c, List.of());
    }

    public static CourseResponse from(Course c, List<UUID> departmentIds) {
        return new CourseResponse(c.getId(), c.getCourseCode(), c.getCourseName(),
                c.getCredit(), c.getEcts(), c.getCreatedAt(), c.getUpdatedAt(), departmentIds);
    }
}
