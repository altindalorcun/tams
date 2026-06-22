package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request body for creating an exemption rule under a department. */
@Schema(description = "Defines a substitution rule: if the student passes all requiredCourseCodes, the exemptedCourseCode is treated as passed by the engine")
public record CreateExemptionRuleRequest(
        @Schema(description = "All these course codes must be passed to trigger the exemption", example = "[\"FIZ103\", \"FIZ104\"]")
        @NotEmpty List<String> requiredCourseCodes,

        @Schema(description = "The course that is exempted when all required courses are passed", example = "FIZ117")
        @NotBlank @Size(max = 20) String exemptedCourseCode
) {}
