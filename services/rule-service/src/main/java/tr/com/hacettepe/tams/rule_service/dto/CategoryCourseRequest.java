package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request body for adding a course to a graduation category. */
@Schema(description = "Request body for assigning a course to a graduation category")
public record CategoryCourseRequest(
        @Schema(description = "ID of the course to assign (must already be in the department's pool)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull UUID courseId,

        @Schema(description = "When true the student must pass this specific course regardless of other thresholds",
                example = "false")
        boolean isMandatory
) {}
