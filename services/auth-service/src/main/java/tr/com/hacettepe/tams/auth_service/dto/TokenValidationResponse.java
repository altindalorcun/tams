package tr.com.hacettepe.tams.auth_service.dto;

import tr.com.hacettepe.tams.auth_service.domain.Role;

import java.util.UUID;

/**
 * Returned by {@code POST /internal/auth/validate}.
 * Consumed by the api-gateway to extract user identity after JWT validation.
 */
public record TokenValidationResponse(
        UUID userId,
        Role role,
        boolean valid
) {}
