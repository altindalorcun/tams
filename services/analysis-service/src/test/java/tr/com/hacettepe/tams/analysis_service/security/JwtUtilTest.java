package tr.com.hacettepe.tams.analysis_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.analysis_service.config.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtil}. No Spring context — JwtUtil and JwtProperties
 * are instantiated directly with a fixed test secret.
 */
class JwtUtilTest {

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LXRlc3Rpbmctb25seQ==";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET));
    }

    private String mintToken(String subject, String role, long expiryMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    @Test
    void isValid_withValidToken_returnsTrue() {
        String token = mintToken("user-id-abc", "TEACHER", 60_000);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() {
        String token = mintToken("user-id-abc", "TEACHER", -1_000);  // already expired
        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_withTamperedSignature_returnsFalse() {
        String valid = mintToken("user-id-abc", "TEACHER", 60_000);
        // Replace last char with a guaranteed-different Base64URL character
        char last = valid.charAt(valid.length() - 1);
        char replacement = (last == 'A') ? 'B' : 'A';
        String tampered = valid.substring(0, valid.length() - 1) + replacement;
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_withMalformedToken_returnsFalse() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void isValid_withBlankToken_returnsFalse() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    void extractRole_returnsRoleClaimFromToken() {
        String token = mintToken("user-id-abc", "ADMIN", 60_000);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractSubject_returnsSubjectFromToken() {
        String token = mintToken("user-uuid-123", "STUDENT", 60_000);
        assertThat(jwtUtil.extractSubject(token)).isEqualTo("user-uuid-123");
    }
}
