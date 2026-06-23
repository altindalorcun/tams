package tr.com.hacettepe.tams.auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import tr.com.hacettepe.tams.auth_service.config.JwtProperties;
import tr.com.hacettepe.tams.auth_service.domain.RefreshToken;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.dto.*;
import tr.com.hacettepe.tams.auth_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;
import tr.com.hacettepe.tams.auth_service.security.JwtUtil;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * All dependencies are mocked — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ACCESS_TOKEN = "header.payload.sig";
    private static final long ACCESS_MS = 900_000L;
    private static final long REFRESH_MS = 604_800_000L;

    private User teacherUser;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(USER_ID)
                .username("teacher1")
                .email("teacher1@test.com")
                .passwordHash("$2a$hashed")
                .role(Role.TEACHER)
                .build();
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — happy path: authenticates and returns token pair")
    void login_happyPath_shouldReturnTokens() {
        LoginRequest req = new LoginRequest("teacher1@test.com", "pass1234");

        when(userRepository.findByEmailOrUsername(req.email(), req.email()))
                .thenReturn(Optional.of(teacherUser));
        when(jwtUtil.generateAccessToken(eq(USER_ID), eq(Role.TEACHER), isNull())).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.username()).isEqualTo("teacher1");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
    }

    @Test
    @DisplayName("login — should propagate BadCredentialsException from AuthenticationManager")
    void login_wrongPassword_shouldPropagateBadCredentials() {
        LoginRequest req = new LoginRequest("teacher1@test.com", "wrongPass");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmailOrUsername(anyString(), anyString());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh — valid token: deletes old token and issues new pair (rotation)")
    void refresh_validToken_shouldRotateAndReturnNewTokens() {
        String rawRefresh = UUID.randomUUID().toString();
        RefreshToken stored = RefreshToken.builder()
                .user(teacherUser)
                .token(rawRefresh)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken(rawRefresh)).thenReturn(Optional.of(stored));
        when(jwtUtil.generateAccessToken(eq(USER_ID), eq(Role.TEACHER), isNull())).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshRequest(rawRefresh));

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.username()).isEqualTo("teacher1");
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh — expired token: deletes token and throws UnauthorizedException")
    void refresh_expiredToken_shouldDeleteAndThrow() {
        String rawRefresh = UUID.randomUUID().toString();
        RefreshToken expired = RefreshToken.builder()
                .user(teacherUser)
                .token(rawRefresh)
                .expiresAt(OffsetDateTime.now().minusHours(1))
                .build();

        when(refreshTokenRepository.findByToken(rawRefresh)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawRefresh)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    @DisplayName("refresh — unknown token: throws UnauthorizedException")
    void refresh_unknownToken_shouldThrow() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("non-existent")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid");
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout — calls deleteByToken with the provided refresh token")
    void logout_shouldDeleteToken() {
        String token = "some-refresh-token";

        authService.logout(token);

        verify(refreshTokenRepository).deleteByToken(token);
    }

    // ── validate ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate — valid Bearer token returns valid=true with userId and role")
    void validate_validBearerToken_shouldReturnValidTrue() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.extractRole(ACCESS_TOKEN)).thenReturn(Role.TEACHER);

        TokenValidationResponse response = authService.validate("Bearer " + ACCESS_TOKEN);

        assertThat(response.valid()).isTrue();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("validate — invalid token returns valid=false with null fields")
    void validate_invalidToken_shouldReturnValidFalse() {
        when(jwtUtil.isValid("bad.token")).thenReturn(false);

        TokenValidationResponse response = authService.validate("Bearer bad.token");

        assertThat(response.valid()).isFalse();
        assertThat(response.userId()).isNull();
        assertThat(response.role()).isNull();
    }

    @Test
    @DisplayName("validate — token without Bearer prefix is also accepted")
    void validate_tokenWithoutBearerPrefix_shouldWork() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.extractRole(ACCESS_TOKEN)).thenReturn(Role.STUDENT);

        TokenValidationResponse response = authService.validate(ACCESS_TOKEN);

        assertThat(response.valid()).isTrue();
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword — correct current password: updates hash and clears sessions")
    void changePassword_correctCurrentPassword_shouldUpdateAndInvalidateSessions() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
        when(passwordEncoder.matches("oldPass", teacherUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPass12")).thenReturn("$2a$newHash");
        when(userRepository.save(any(User.class))).thenReturn(teacherUser);

        authService.changePassword(USER_ID, new ChangePasswordRequest("oldPass", "newPass12"));

        verify(userRepository).save(argThat(u ->
                u.getPasswordHash().equals("$2a$newHash") && !u.isMustChangePassword()
        ));
        verify(refreshTokenRepository).deleteAllByUser(teacherUser);
    }

    @Test
    @DisplayName("changePassword — wrong current password: throws UnauthorizedException")
    void changePassword_wrongCurrentPassword_shouldThrow() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
        when(passwordEncoder.matches("wrongPass", teacherUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword(USER_ID, new ChangePasswordRequest("wrongPass", "newPass12")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteAllByUser(any());
    }
}
