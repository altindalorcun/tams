package tr.com.hacettepe.tams.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/auth/change-password.
 * The current password must be provided to prevent unauthorized changes.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
