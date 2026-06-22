package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a course in the global catalog. */
@Schema(description = "Request body for creating a course in the institution-wide catalog")
public record CreateCourseRequest(
        @Schema(description = "Globally unique course code", example = "MAT101")
        @NotBlank @Size(max = 20) String courseCode,

        @Schema(description = "Full course name", example = "Calculus I")
        @NotBlank @Size(max = 255) String courseName,

        @Schema(description = "Credit value (zero or positive)", example = "4.00")
        @PositiveOrZero BigDecimal credit,

        @Schema(description = "ECTS value (must be positive)", example = "5.00")
        @PositiveOrZero BigDecimal ects
) {}
