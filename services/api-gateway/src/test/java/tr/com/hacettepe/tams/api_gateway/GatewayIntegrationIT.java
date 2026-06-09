package tr.com.hacettepe.tams.api_gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Integration tests for the API gateway JWT filter and routing behaviour.
 *
 * <p>WireMock acts as the downstream for all routes so tests never reach real services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class GatewayIntegrationIT {

    private static final String TEST_JWT_SECRET =
            "dGVzdHNlY3JldGtleXRlc3RzZWNyZXRrZXl0ZXN0c2VjcmV0a2V5dGVzdHNlY3JldGtleXRlc3Q=";
    private static final long TOKEN_VALIDITY_MS = 900_000L;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetWireMock() {
        WireMock.reset();
    }

    // ─── Auth path — no JWT required ──────────────────────────────────────────

    @Test
    void authLogin_withoutJwt_routesThrough() throws Exception {
        stubFor(post(urlPathEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"accessToken\":\"tok\",\"refreshToken\":\"ref\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk());
    }

    // ─── Protected path — valid JWT required ──────────────────────────────────

    @Test
    void protectedPath_withValidTeacherJwt_routesThrough() throws Exception {
        UUID teacherId = UUID.randomUUID();
        String token = mintToken("TEACHER", teacherId);

        stubFor(get(urlPathEqualTo("/api/v1/results"))
                .withHeader("X-User-Id", equalTo(teacherId.toString()))
                .withHeader("X-User-Role", equalTo("TEACHER"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"content\":[]}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/results")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPath_withoutJwt_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/results"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.detail").value(containsString("JWT")));
    }

    @Test
    void protectedPath_withMalformedJwt_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/results")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedPath_withExpiredJwt_returns401() throws Exception {
        String expired = mintExpiredToken("TEACHER", UUID.randomUUID());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/results")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ─── Unknown path — no matching route ─────────────────────────────────────

    @Test
    void unknownPath_withValidJwt_returns404() throws Exception {
        String token = mintToken("ADMIN", UUID.randomUUID());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/nonexistent-service/foo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ─── X-User-Id / X-User-Role propagation ─────────────────────────────────

    @Test
    void validJwt_propagatesIdentityHeaders_toDownstream() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = mintToken("ADMIN", userId);

        stubFor(get(urlPathEqualTo("/api/v1/departments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        verify(getRequestedFor(urlPathEqualTo("/api/v1/departments"))
                .withHeader("X-User-Id", equalTo(userId.toString()))
                .withHeader("X-User-Role", equalTo("ADMIN")));
    }

    // ─── Actuator — no JWT required ───────────────────────────────────────────

    @Test
    void actuatorHealth_withoutJwt_isAccessible() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String mintToken(String role, UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(key)
                .compact();
    }

    private String mintExpiredToken(String role, UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));
        long past = System.currentTimeMillis() - 3_600_000L;
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date(past - 60_000L))
                .expiration(new Date(past))
                .signWith(key)
                .compact();
    }
}
