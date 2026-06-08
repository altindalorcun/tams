package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for updating an existing department. */
@Schema(description = "Request body for updating an existing department")
public record UpdateDepartmentRequest(
        @Schema(description = "New unique name for the department", example = "Elektrik-Elektronik Mühendisliği")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Updated description", example = "Department of Electrical and Electronics Engineering")
        String description
) {}
