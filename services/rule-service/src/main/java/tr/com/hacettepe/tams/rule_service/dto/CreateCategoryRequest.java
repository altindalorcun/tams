package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Request body for creating a graduation category under a department. */
@Schema(description = "Request body for creating a graduation requirement category under a department")
public record CreateCategoryRequest(
        @Schema(description = "Category name, unique within its department", example = "Teknik Seçmeli")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Optional description", example = "Technical elective courses")
        String description,

        @Schema(description = "Minimum total credits required from this category's course pool", example = "12.00")
        @NotNull @PositiveOrZero BigDecimal minCredit,

        @Schema(description = "Minimum total ECTS required from this category's course pool", example = "18.00")
        @NotNull @PositiveOrZero BigDecimal minEcts,

        @Schema(description = "Minimum number of courses the student must pass from this category", example = "4")
        @Min(0) int minCourseCount,

        @Schema(description = "First enrollment year this category applies to (null = no lower bound)", example = "2015")
        Integer appliesFromYear,

        @Schema(description = "Last enrollment year this category applies to (null = no upper bound)", example = "2025")
        Integer appliesToYear,

        @Schema(description = "Course codes that trigger alternative thresholds when any of them is passed by the student")
        List<String> conditionCourseCodes,

        @Schema(description = "Alternative min course count used when at least one condition course is passed (null = use base threshold)")
        @Min(0) Integer minCourseCountIfMet,

        @Schema(description = "Alternative min ECTS used when at least one condition course is passed (null = use base threshold)")
        @PositiveOrZero BigDecimal minEctsIfMet
) {}
