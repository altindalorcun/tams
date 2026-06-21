package tr.com.hacettepe.tams.auth_service.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.com.hacettepe.tams.auth_service.domain.Role;

/**
 * Request body for admin-initiated user creation.
 * No password field — a secure default is assigned by the service.
 * The user is required to change it on first login.
 */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email String email,
        @NotNull Role role,
        @Nullable @Size(max = 20) String studentNumber
) {}
