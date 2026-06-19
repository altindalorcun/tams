package tr.com.hacettepe.tams.auth_service.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.auth_service.config.JwtProperties;
import tr.com.hacettepe.tams.auth_service.domain.Role;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 * No Spring context — properties are constructed directly.
 */
class JwtUtilTest {

    private static final String TEST_SECRET = "6eBC12u4cG3WAfePlx21HNUfkGxCR/M39oqNtDfwzQs=";
    private static final long ACCESS_MS = 900_000L;
    private static final long REFRESH_MS = 604_800_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, ACCESS_MS, REFRESH_MS));
    }

    @Test
    @DisplayName("Generated token subject should equal the user UUID")
    void generateAccessToken_subjectShouldBeUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, Role.TEACHER, null);

        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("Generated token should contain the correct role claim")
    void generateAccessToken_shouldContainRoleClaim() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, Role.STUDENT, null);

        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("role", String.class)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("STUDENT token should include studentNumber claim when provided")
    void generateAccessToken_studentShouldContainStudentNumberClaim() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, Role.STUDENT, "20190001");

        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("studentNumber", String.class)).isEqualTo("20190001");
    }

    @Test
    @DisplayName("Non-STUDENT token should not include studentNumber claim")
    void generateAccessToken_nonStudentShouldNotContainStudentNumberClaim() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), Role.TEACHER, null);

        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("studentNumber")).isNull();
    }

    @Test
    @DisplayName("Generated token expiry should be approximately ACCESS_MS milliseconds in the future")
    void generateAccessToken_expiryShouldBeInFuture() {
        long beforeMs = System.currentTimeMillis();
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), Role.ADMIN, null);

        Date expiry = jwtUtil.parseToken(token).getExpiration();
        long afterMs = System.currentTimeMillis();

        // JWT stores exp in whole seconds, so the actual expiry can be up to 999 ms
        // less than beforeMs + ACCESS_MS due to millisecond truncation.
        assertThat(expiry.getTime())
                .isGreaterThanOrEqualTo(beforeMs + ACCESS_MS - 1000)
                .isLessThanOrEqualTo(afterMs + ACCESS_MS + 1000);
    }

    @Test
    @DisplayName("extractUserId should return the UUID embedded in the token")
    void extractUserId_shouldReturnCorrectUUID() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, Role.TEACHER, null);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("extractRole should return the role embedded in the token")
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), Role.ADMIN, null);

        assertThat(jwtUtil.extractRole(token)).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("isValid should return true for a freshly generated token")
    void isValid_shouldReturnTrue_forValidToken() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), Role.TEACHER, null);

        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid should return false for a token that expired in the past")
    void isValid_shouldReturnFalse_forExpiredToken() {
        // Use negative expiration to produce an already-expired token
        JwtUtil expiredJwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, -1000L, REFRESH_MS));
        String expiredToken = expiredJwtUtil.generateAccessToken(UUID.randomUUID(), Role.STUDENT, null);

        assertThat(jwtUtil.isValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for a token signed with a different secret")
    void isValid_shouldReturnFalse_forTokenSignedWithDifferentSecret() {
        String differentSecret = "ZGlmZmVyZW50U2VjcmV0S2V5VGhhdElzTG9uZ0Vub3VnaA==";
        JwtUtil otherJwtUtil = new JwtUtil(new JwtProperties(differentSecret, ACCESS_MS, REFRESH_MS));
        String foreignToken = otherJwtUtil.generateAccessToken(UUID.randomUUID(), Role.TEACHER, null);

        assertThat(jwtUtil.isValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for a tampered token")
    void isValid_shouldReturnFalse_forTamperedToken() {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), Role.TEACHER, null);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for an empty string")
    void isValid_shouldReturnFalse_forEmptyString() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid should return false for a completely random string")
    void isValid_shouldReturnFalse_forGarbage() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }
}
