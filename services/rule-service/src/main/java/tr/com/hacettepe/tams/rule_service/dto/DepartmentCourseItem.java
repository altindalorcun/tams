package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.rule_service.domain.Course;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A course that belongs to a department's pool.
 * Uses {@code courseId} (not {@code id}) to distinguish it from the global course catalog
 * and to match the shape the frontend expects.
 */
@Schema(description = "A course that has been assigned to a department's offering pool")
public record DepartmentCourseItem(
        @Schema(description = "Course UUID") UUID courseId,
        @Schema(description = "Catalog code, e.g. BBM101") String courseCode,
        @Schema(description = "Full course name") String courseName,
        @Schema(description = "Credit hours") BigDecimal credit,
        @Schema(description = "ECTS credits") BigDecimal ects
) {
    /** Creates a {@code DepartmentCourseItem} from a {@link Course} entity. */
    public static DepartmentCourseItem from(Course c) {
        return new DepartmentCourseItem(c.getId(), c.getCourseCode(), c.getCourseName(),
                c.getCredit(), c.getEcts());
    }
}
