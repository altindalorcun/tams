package tr.com.hacettepe.tams.rule_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Base class for integration tests.
 *
 * <p>Uses the <em>singleton container</em> pattern: a single PostgreSQL container is
 * started once in a static initializer and shared across every test class for the
 * whole JVM. It is intentionally never stopped per class — Spring caches the
 * application context across test classes, so a per-class container (the
 * {@code @Testcontainers} lifecycle) would be torn down while a cached context still
 * points at it, causing "connection refused" failures in subsequent classes.
 *
 * <p>Flyway migrations run against this container on context startup, so the schema
 * is always current. {@link #bearerToken(String)} mints a JWT signed with the same
 * shared secret the service validates against, exercising the real
 * {@code JwtAuthenticationFilter} rather than mocking authentication.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final long TOKEN_VALIDITY_MS = 900_000L;

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Mints a signed JWT carrying the given role claim, valid for 15 minutes.
     *
     * @param role the role claim (e.g. {@code ADMIN}, {@code TEACHER})
     * @return a compact JWT string ready for the {@code Authorization: Bearer} header
     */
    protected String bearerToken(String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.builder()
                .subject("test-user-id")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(key)
                .compact();
    }
}
