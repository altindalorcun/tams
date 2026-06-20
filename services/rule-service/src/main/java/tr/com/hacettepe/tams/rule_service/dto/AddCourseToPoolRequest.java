package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request body for adding a course to a department's offering pool. */
@Schema(description = "Request body for adding a course to a department's course pool")
public record AddCourseToPoolRequest(
        @Schema(description = "ID of the course to add", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull UUID courseId
) {}
