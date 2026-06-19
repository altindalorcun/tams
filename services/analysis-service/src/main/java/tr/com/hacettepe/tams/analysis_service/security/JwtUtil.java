package tr.com.hacettepe.tams.analysis_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.config.JwtProperties;

import javax.crypto.SecretKey;

/**
 * Validates and parses JWT access tokens issued by auth-service.
 * analysis-service shares the same secret key but never issues tokens.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_STUDENT_NUMBER = "studentNumber";

    private final JwtProperties jwtProperties;

    /**
     * Parses the token and returns its claims.
     * Throws {@link JwtException} for any signature, expiry, or format issue.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractRole(String token) {
        return parseToken(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Extracts the {@code studentNumber} claim from a STUDENT token.
     * Returns {@code null} for non-STUDENT tokens (claim absent).
     */
    public String extractStudentNumber(String token) {
        return parseToken(token).get(CLAIM_STUDENT_NUMBER, String.class);
    }

    public String extractSubject(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }
}
