package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating a new department. */
public record CreateDepartmentRequest(
        @NotBlank @Size(max = 255) String name,
        String description
) {}
