package tr.com.hacettepe.tams.auth_service.service;

import tr.com.hacettepe.tams.auth_service.dto.*;

import java.util.UUID;

/**
 * Core authentication operations: login, token management, and password change.
 * Implementations must never log raw passwords or tokens.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(String refreshToken);

    TokenValidationResponse validate(String bearerToken);

    /**
     * Changes the authenticated user's password and invalidates all existing
     * refresh tokens to force a fresh login after the change.
     *
     * @param userId          the ID of the currently authenticated user
     * @param request         current and new password pair
     */
    void changePassword(UUID userId, ChangePasswordRequest request);
}
