package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for updating an existing department. */
public record UpdateDepartmentRequest(
        @NotBlank @Size(max = 255) String name,
        String description
) {}
