package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated response for the department course-pool screen.
 * Returns both assigned and available courses in a single request,
 * eliminating the need for two separate API calls from the frontend.
 */
@Schema(description = "Assigned and available courses for a department's offering pool")
public record DepartmentCoursePoolResponse(
        @Schema(description = "Courses already in this department's pool")
        List<DepartmentCourseItem> assignedCourses,

        @Schema(description = "Courses in the global catalog not yet assigned to this department")
        List<CourseResponse> availableCourses
) {}
