package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Request body for updating a graduation category. */
@Schema(description = "Request body for updating an existing graduation category")
public record UpdateCategoryRequest(
        @Schema(description = "New category name, unique within its department", example = "Teknik Seçmeli (Grup A)")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Updated description")
        String description,

        @Schema(description = "Updated minimum total credits threshold", example = "15.00")
        @NotNull @PositiveOrZero BigDecimal minCredit,

        @Schema(description = "Updated minimum total ECTS threshold", example = "21.00")
        @NotNull @PositiveOrZero BigDecimal minEcts,

        @Schema(description = "Updated minimum course count", example = "5")
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
