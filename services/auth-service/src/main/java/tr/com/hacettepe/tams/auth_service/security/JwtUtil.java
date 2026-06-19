package tr.com.hacettepe.tams.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.auth_service.config.JwtProperties;
import tr.com.hacettepe.tams.auth_service.domain.Role;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for creating and validating JWT access tokens.
 * Refresh tokens are opaque strings persisted in the database — this
 * class only handles access tokens (short-lived JWTs).
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_STUDENT_NUMBER = "studentNumber";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UUID userId, Role role, String studentNumber) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessExpirationMs());

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(expiry);

        if (role == Role.STUDENT && studentNumber != null) {
            builder.claim(CLAIM_STUDENT_NUMBER, studentNumber);
        }

        return builder.signWith(secretKey()).compact();
    }

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

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public Role extractRole(String token) {
        return Role.valueOf(parseToken(token).get(CLAIM_ROLE, String.class));
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
