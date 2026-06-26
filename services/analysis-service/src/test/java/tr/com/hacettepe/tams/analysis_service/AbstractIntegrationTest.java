package tr.com.hacettepe.tams.analysis_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import tr.com.hacettepe.tams.analysis_service.client.RuleServiceClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Base class for analysis-service integration tests.
 *
 * <p>The {@link DockerAvailableCondition} extension disables the entire test class
 * (before Spring context loading) when a Docker daemon is not reachable, avoiding
 * context failures in sandbox or CI environments without Docker.
 *
 * <p>Uses the singleton container pattern: one PostgreSQL container is started once
 * per JVM and shared across all IT classes. Spring caches the application context
 * so Flyway migrations run only once.
 *
 * <p>{@link RuleServiceClient} is mocked at this level so that
 * {@code TranscriptParsedConsumer} never calls the real rule-service during tests.
 */
@ExtendWith(DockerAvailableCondition.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"transcript.raw", "transcript.parsed"},
        brokerPropertiesLocation = "classpath:kafka-test.properties"
)
public abstract class AbstractIntegrationTest {

    private static final long TOKEN_VALIDITY_MS = 900_000L;

    /** Prevents the consumer from calling the real rule-service during tests. */
    @MockBean
    protected RuleServiceClient ruleServiceClient;

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    protected String bearerToken(String role, UUID userId) {
        return bearerToken(role, userId, null);
    }

    /**
     * Mints a signed JWT carrying the given role claim, valid for 15 minutes.
     *
     * @param role          the role claim (e.g. {@code TEACHER}, {@code ADMIN}, {@code STUDENT})
     * @param userId        the UUID to use as the subject (the caller's userId in auth-service)
     * @param studentNumber optional student number claim for STUDENT tokens
     * @return a compact JWT string ready for the {@code Authorization: Bearer} header
     */
    protected String bearerToken(String role, UUID userId, String studentNumber) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS));
        if ("STUDENT".equals(role) && studentNumber != null) {
            builder.claim("studentNumber", studentNumber);
        }
        return builder.signWith(key).compact();
    }
}
