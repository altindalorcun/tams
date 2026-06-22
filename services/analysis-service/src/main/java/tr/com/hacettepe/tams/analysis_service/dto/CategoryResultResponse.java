package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.CategoryResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Read-only representation of a single graduation requirement category evaluation.
 * Both satisfied and unsatisfied categories are returned, enabling per-category
 * progress bars in the frontend.
 */
@Schema(description = "Evaluation outcome for one graduation requirement category")
public record CategoryResultResponse(
        @Schema(description = "UUID of the graduation category in rule-service")
        UUID categoryId,
        @Schema(description = "Display name of the category", example = "Temel Bilgisayar Bilimleri")
        String categoryName,
        @Schema(description = "True when all thresholds and mandatory courses are satisfied")
        boolean satisfied,
        @Schema(description = "Minimum credit hours required by the rule set", example = "30.0")
        BigDecimal requiredCredit,
        @Schema(description = "Credit hours earned by the student in this category", example = "18.0")
        BigDecimal earnedCredit,
        @Schema(description = "Minimum ECTS required by the rule set", example = "50.0")
        BigDecimal requiredEcts,
        @Schema(description = "ECTS earned by the student in this category", example = "32.0")
        BigDecimal earnedEcts,
        @Schema(description = "Minimum number of courses required", example = "5")
        int requiredCourseCount,
        @Schema(description = "Number of courses passed in this category's pool", example = "3")
        int earnedCourseCount,
        @Schema(description = "Course codes that are mandatory but not yet passed", example = "[\"CS101\", \"CS201\"]")
        List<String> missingMandatoryCourses,
        @Schema(description = "True when this category does not apply to the student's enrollment cohort")
        boolean cohortSkipped
) {
    /**
     * Factory method that maps a {@link CategoryResult} entity to this response record.
     */
    public static CategoryResultResponse from(CategoryResult cr) {
        List<String> missing = cr.getMissingMandatoryCourses() != null
                ? Arrays.asList(cr.getMissingMandatoryCourses())
                : List.of();
        return new CategoryResultResponse(
                cr.getCategoryId(),
                cr.getCategoryName(),
                cr.isSatisfied(),
                cr.getRequiredCredit(),
                cr.getEarnedCredit(),
                cr.getRequiredEcts(),
                cr.getEarnedEcts(),
                cr.getRequiredCourseCount(),
                cr.getEarnedCourseCount(),
                missing,
                cr.isCohortSkipped()
        );
    }
}
