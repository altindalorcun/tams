package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/** Request body for updating an existing course assignment within a graduation category. */
@Schema(description = "Request body for updating a course assignment in a graduation category")
public record UpdateCategoryCourseRequest(
        @Schema(description = "When true the student must pass this course when their cohort is within the applicability range",
                example = "false")
        boolean isMandatory,

        @Schema(description = "First enrollment year for which this assignment applies (inclusive; null = no lower bound)",
                example = "2017")
        Integer appliesFromYear,

        @Schema(description = "Term within appliesFromYear: GUZ or BAHAR (null defaults to GUZ)",
                example = "GUZ")
        @Pattern(regexp = "GUZ|BAHAR", message = "appliesFromTerm must be GUZ or BAHAR")
        String appliesFromTerm,

        @Schema(description = "First enrollment year from which this assignment no longer applies (exclusive; null = no upper bound)",
                example = "2017")
        Integer appliesToYear,

        @Schema(description = "Term within appliesToYear: GUZ or BAHAR (null defaults to GUZ)",
                example = "GUZ")
        @Pattern(regexp = "GUZ|BAHAR", message = "appliesToTerm must be GUZ or BAHAR")
        String appliesToTerm
) {}
