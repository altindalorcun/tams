package tr.com.hacettepe.tams.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tr.com.hacettepe.tams.auth_service.dto.*;
import tr.com.hacettepe.tams.auth_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.auth_service.service.AuthService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, logout, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with e-mail or username")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the presented refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password; invalidates all sessions")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        UUID userId = UUID.fromString(authentication.getName());
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
