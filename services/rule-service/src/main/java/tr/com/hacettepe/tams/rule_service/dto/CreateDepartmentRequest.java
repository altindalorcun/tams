package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating a new department. */
@Schema(description = "Request body for creating a new department")
public record CreateDepartmentRequest(
        @Schema(description = "Unique department name", example = "Bilgisayar Mühendisliği")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Optional description", example = "Department of Computer Engineering")
        String description
) {}
