package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a graduation category under a department. */
public record CreateCategoryRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal minCredit,
        @NotNull @PositiveOrZero BigDecimal minEcts,
        @Min(0) int minCourseCount
) {}
