package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated response for the category course-pool screen.
 * Returns assigned and available courses in a single request so the frontend
 * can populate both lists without client-side cross-filtering.
 */
@Schema(description = "Assigned and available courses for a graduation category's pool")
public record CategoryCoursePoolResponse(
        @Schema(description = "Courses already assigned to this category")
        List<CategoryCourseResponse> assignedCourses,

        @Schema(description = "Courses in the department pool not yet assigned to this category")
        List<DepartmentCourseItem> availableCourses
) {}
