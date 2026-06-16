package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a course in the global catalog. */
@Schema(description = "Request body for creating a course in the institution-wide catalog")
public record CreateCourseRequest(
        @Schema(description = "Globally unique course code", example = "MAT101")
        @NotBlank @Size(max = 20) String courseCode,

        @Schema(description = "Full course name", example = "Calculus I")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Credit value (must be positive)", example = "4.00")
        @Positive BigDecimal credits,

        @Schema(description = "ECTS value (must be positive)", example = "5.00")
        @Positive BigDecimal ects
) {}
