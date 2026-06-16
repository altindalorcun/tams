package tr.com.hacettepe.tams.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.auth_service.config.JwtProperties;
import tr.com.hacettepe.tams.auth_service.domain.RefreshToken;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.dto.*;
import tr.com.hacettepe.tams.auth_service.exception.ConflictException;
import tr.com.hacettepe.tams.auth_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.auth_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;
import tr.com.hacettepe.tams.auth_service.security.JwtUtil;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Handles all authentication and token lifecycle operations.
 * Refresh token rotation: every successful /refresh call deletes the
 * presented token and issues a brand-new one, limiting the damage from
 * token theft to a single use window.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new UnauthorizedException("ADMIN accounts cannot be self-registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        user = userRepository.save(user);
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token has expired — please log in again");
        }

        // Rotate: delete the used token, issue a new pair
        refreshTokenRepository.delete(stored);
        return issueTokenPair(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    @Override
    public TokenValidationResponse validate(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!jwtUtil.isValid(token)) {
            return new TokenValidationResponse(null, null, false);
        }
        UUID userId = jwtUtil.extractUserId(token);
        Role role = jwtUtil.extractRole(token);
        return new TokenValidationResponse(userId, role, true);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(OffsetDateTime.now()
                        .plusNanos(jwtProperties.refreshExpirationMs() * 1_000_000L))
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, rawRefreshToken,
                jwtProperties.accessExpirationMs(), user.getId(), user.getRole());
    }

    private String stripBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
