package tr.com.hacettepe.tams.auth_service.service;

import tr.com.hacettepe.tams.auth_service.dto.*;

/**
 * Core authentication operations: registration, login, token management.
 * Implementations must never log raw passwords or tokens.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(String refreshToken);

    TokenValidationResponse validate(String bearerToken);
}
