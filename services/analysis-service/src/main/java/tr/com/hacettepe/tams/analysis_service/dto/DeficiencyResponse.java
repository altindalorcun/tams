package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.Deficiency;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Read-only representation of a single unfulfilled graduation requirement category.
 */
@Schema(description = "Details of one unsatisfied graduation requirement category")
public record DeficiencyResponse(
        @Schema(description = "Primary key of this deficiency record")
        UUID id,
        @Schema(description = "Name of the graduation category that is not satisfied", example = "Temel Bilgisayar Bilimleri")
        String categoryName,
        @Schema(description = "Minimum credit hours required by the rule set", example = "30.0")
        BigDecimal requiredCredit,
        @Schema(description = "Credit hours actually earned by the student in this category", example = "18.0")
        BigDecimal earnedCredit,
        @Schema(description = "Minimum ECTS required by the rule set", example = "50.0")
        BigDecimal requiredEcts,
        @Schema(description = "ECTS actually earned by the student in this category", example = "32.0")
        BigDecimal earnedEcts,
        @Schema(description = "Course codes that are mandatory but not yet passed", example = "[\"CS101\", \"CS201\"]")
        List<String> missingCourses
) {
    public static DeficiencyResponse from(Deficiency d) {
        List<String> courses = d.getMissingCourses() != null
                ? Arrays.asList(d.getMissingCourses())
                : List.of();
        return new DeficiencyResponse(
                d.getId(),
                d.getCategoryName(),
                d.getRequiredCredit(),
                d.getEarnedCredit(),
                d.getRequiredEcts(),
                d.getEarnedEcts(),
                courses
        );
    }
}
