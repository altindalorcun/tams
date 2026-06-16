package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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
        @Min(0) int minCourseCount
) {}
