package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for updating a course in the global catalog. */
@Schema(description = "Request body for updating an existing course")
public record UpdateCourseRequest(
        @Schema(description = "New globally unique course code", example = "MAT102")
        @NotBlank @Size(max = 20) String courseCode,

        @Schema(description = "Updated course name", example = "Calculus II")
        @NotBlank @Size(max = 255) String courseName,

        @Schema(description = "Updated credits value (must be positive)", example = "3.00")
        @Positive BigDecimal credit,

        @Schema(description = "Updated ECTS value (must be positive)", example = "4.00")
        @Positive BigDecimal ects
) {}
