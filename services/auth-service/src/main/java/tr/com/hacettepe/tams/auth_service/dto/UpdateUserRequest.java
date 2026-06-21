package tr.com.hacettepe.tams.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating an existing user's profile.
 * Role is intentionally excluded — role changes are not permitted after creation.
 */
public record UpdateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email String email,
        @NotNull Boolean isActive
) {}
