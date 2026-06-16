package tr.com.hacettepe.tams.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login with either e-mail address or username.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
