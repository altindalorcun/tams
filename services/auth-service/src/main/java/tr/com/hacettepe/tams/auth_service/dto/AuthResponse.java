package tr.com.hacettepe.tams.auth_service.dto;

import tr.com.hacettepe.tams.auth_service.domain.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        Role role
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresInMs, UUID userId, Role role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer",
                expiresInMs / 1000, userId, role);
    }
}
