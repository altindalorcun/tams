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
import tr.com.hacettepe.tams.auth_service.exception.ConflictException;
import tr.com.hacettepe.tams.auth_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;
import tr.com.hacettepe.tams.auth_service.security.JwtUtil;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — happy path: saves user and returns token pair")
    void register_happyPath_shouldSaveUserAndReturnTokens() {
        RegisterRequest req = new RegisterRequest("teacher1", "teacher1@test.com", "pass1234", Role.TEACHER);

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUsername(req.username())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(teacherUser);
        when(jwtUtil.generateAccessToken(USER_ID, Role.TEACHER)).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.role()).isEqualTo(Role.TEACHER);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.refreshToken()).isNotBlank();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(req.password());
    }

    @Test
    @DisplayName("register — password is hashed; plain text must never be passed to save()")
    void register_passwordShouldBeHashed() {
        RegisterRequest req = new RegisterRequest("student1", "student1@test.com", "plainPass8", Role.STUDENT);
        User student = User.builder().id(UUID.randomUUID()).username("student1")
                .email("student1@test.com").passwordHash("$2a$bcrypt").role(Role.STUDENT).build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainPass8")).thenReturn("$2a$bcrypt");
        when(userRepository.save(any(User.class))).thenReturn(student);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("$2a$bcrypt") &&
                !user.getPasswordHash().equals("plainPass8")
        ));
    }

    @Test
    @DisplayName("register — should throw ConflictException when email is already taken")
    void register_duplicateEmail_shouldThrowConflict() {
        RegisterRequest req = new RegisterRequest("teacher2", "taken@test.com", "pass1234", Role.TEACHER);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("taken@test.com");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — should throw ConflictException when username is already taken")
    void register_duplicateUsername_shouldThrowConflict() {
        RegisterRequest req = new RegisterRequest("takenUser", "new@test.com", "pass1234", Role.TEACHER);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("takenUser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("takenUser");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — should throw UnauthorizedException when role is ADMIN")
    void register_adminRole_shouldThrowUnauthorized() {
        RegisterRequest req = new RegisterRequest("hacker", "hacker@test.com", "pass1234", Role.ADMIN);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("ADMIN");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — happy path: authenticates and returns token pair")
    void login_happyPath_shouldReturnTokens() {
        LoginRequest req = new LoginRequest("teacher1@test.com", "pass1234");

        when(userRepository.findByEmailOrUsername(req.identifier(), req.identifier()))
                .thenReturn(Optional.of(teacherUser));
        when(jwtUtil.generateAccessToken(USER_ID, Role.TEACHER)).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.userId()).isEqualTo(USER_ID);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(req.identifier(), req.password())
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
        when(jwtUtil.generateAccessToken(USER_ID, Role.TEACHER)).thenReturn(ACCESS_TOKEN);
        when(jwtProperties.refreshExpirationMs()).thenReturn(REFRESH_MS);
        when(jwtProperties.accessExpirationMs()).thenReturn(ACCESS_MS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshRequest(rawRefresh));

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        // Old token must be deleted
        verify(refreshTokenRepository).delete(stored);
        // A new token must be persisted
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
}
