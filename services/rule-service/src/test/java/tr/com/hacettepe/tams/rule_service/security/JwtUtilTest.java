package tr.com.hacettepe.tams.rule_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.rule_service.config.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtil}.
 * Exercises the shared-secret validation path that the security filter relies on:
 * role/subject extraction, signature checks, expiry, and malformed input.
 */
class JwtUtilTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LXRlc3Rpbmctb25seQ==";
    private static final String OTHER_SECRET =
            "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LWFuZC1kaWZmZXJlbnQ=";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtProperties(SECRET));
    }

    @Test
    @DisplayName("extractRole / extractSubject — returns claims from a valid token")
    void extractsClaims() {
        String token = sign(SECRET, "user-42", "ADMIN", 60_000L);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractSubject(token)).isEqualTo("user-42");
    }

    @Test
    @DisplayName("isValid — true for a correctly signed, unexpired token")
    void isValid_validToken_true() {
        assertThat(jwtUtil.isValid(sign(SECRET, "user", "TEACHER", 60_000L))).isTrue();
    }

    @Test
    @DisplayName("isValid — false for a token signed with a different secret")
    void isValid_wrongSignature_false() {
        assertThat(jwtUtil.isValid(sign(OTHER_SECRET, "user", "ADMIN", 60_000L))).isFalse();
    }

    @Test
    @DisplayName("isValid — false for an expired token")
    void isValid_expiredToken_false() {
        assertThat(jwtUtil.isValid(sign(SECRET, "user", "ADMIN", -1_000L))).isFalse();
    }

    @Test
    @DisplayName("isValid — false for a malformed / non-JWT string")
    void isValid_malformed_false() {
        assertThat(jwtUtil.isValid("not-a-jwt")).isFalse();
    }

    private static String sign(String secret, String subject, String role, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }
}
