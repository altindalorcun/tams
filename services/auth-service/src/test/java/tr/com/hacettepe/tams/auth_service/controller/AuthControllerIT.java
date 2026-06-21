package tr.com.hacettepe.tams.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.auth_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.auth_service.domain.RefreshToken;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.dto.LoginRequest;
import tr.com.hacettepe.tams.auth_service.dto.RefreshRequest;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController} endpoints.
 * Runs against a real PostgreSQL container (via AbstractIntegrationTest).
 * Users are created directly via the repository since the public register
 * endpoint has been removed in favour of admin-managed user creation.
 */
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String LOGIN_URL   = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL  = "/api/v1/auth/logout";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .forEach(u -> userRepository.deleteById(u.getId()));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login — valid email identifier → 200 with tokens")
    void login_withEmail_shouldReturn200() throws Exception {
        createUser("loginUser", "login@test.com", "pass1234!", Role.TEACHER);

        LoginRequest login = new LoginRequest("login@test.com", "pass1234!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.mustChangePassword").isBoolean());
    }

    @Test
    @DisplayName("POST /login — valid username identifier → 200 with tokens")
    void login_withUsername_shouldReturn200() throws Exception {
        createUser("loginUser2", "login2@test.com", "pass1234!", Role.STUDENT, "20231002");

        LoginRequest login = new LoginRequest("loginUser2", "pass1234!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /login — wrong password → 401 Unauthorized")
    void login_wrongPassword_shouldReturn401() throws Exception {
        createUser("loginUser3", "login3@test.com", "correctPass1!", Role.TEACHER);

        LoginRequest login = new LoginRequest("login3@test.com", "wrongPassword");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login — non-existent user → 401 Unauthorized")
    void login_unknownUser_shouldReturn401() throws Exception {
        LoginRequest login = new LoginRequest("nobody@nowhere.com", "anyPass123");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /refresh — valid refresh token → 200 with new token pair (rotation)")
    void refresh_validToken_shouldReturn200WithNewPair() throws Exception {
        String refreshToken = loginAndGetRefreshToken("refreshUser", "refresh@test.com", Role.TEACHER);

        RefreshRequest body = new RefreshRequest(refreshToken);

        String responseJson = mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newRefreshToken = objectMapper.readTree(responseJson).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("POST /refresh — expired token in DB → 401 Unauthorized")
    void refresh_expiredToken_shouldReturn401() throws Exception {
        String refreshToken = loginAndGetRefreshToken("expiredUser", "expired@test.com", Role.TEACHER);

        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setExpiresAt(OffsetDateTime.now().minusHours(1));
            refreshTokenRepository.save(rt);
        });

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("POST /refresh — unknown token → 401 Unauthorized")
    void refresh_unknownToken_shouldReturn401() throws Exception {
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("ghost-token"))))
                .andExpect(status().isUnauthorized());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /logout — valid refresh token → 204 No Content, token removed from DB")
    void logout_shouldReturn204AndRemoveToken() throws Exception {
        String refreshToken = loginAndGetRefreshToken("logoutUser", "logout@test.com", Role.TEACHER);

        mockMvc.perform(post(LOGOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("POST /logout — unknown token → 204 No Content (idempotent)")
    void logout_unknownToken_shouldReturn204() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("ghost-token"))))
                .andExpect(status().isNoContent());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User createUser(String username, String email, String password, Role role) {
        return createUser(username, email, password, role, null);
    }

    private User createUser(String username, String email, String password, Role role, String studentNumber) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .studentNumber(studentNumber)
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetRefreshToken(String username, String email, Role role) throws Exception {
        createUser(username, email, "pass1234!", role);
        LoginRequest login = new LoginRequest(email, "pass1234!");

        String responseJson = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).get("refreshToken").asText();
    }
}
