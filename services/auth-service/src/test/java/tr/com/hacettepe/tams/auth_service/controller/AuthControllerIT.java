package tr.com.hacettepe.tams.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.auth_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.auth_service.domain.RefreshToken;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.dto.LoginRequest;
import tr.com.hacettepe.tams.auth_service.dto.RefreshRequest;
import tr.com.hacettepe.tams.auth_service.dto.RegisterRequest;
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
 * Each test starts with a clean slate: all non-admin users and tokens are removed.
 */
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REFRESH_URL  = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL   = "/api/v1/auth/logout";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .forEach(u -> userRepository.deleteById(u.getId()));
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register — TEACHER role → 201 with tokens")
    void register_teacher_shouldReturn201WithTokens() throws Exception {
        RegisterRequest body = new RegisterRequest("teacher1", "teacher1@test.com", "pass1234!", Role.TEACHER, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /register — STUDENT role with studentNumber → 201 with tokens")
    void register_student_shouldReturn201() throws Exception {
        RegisterRequest body = new RegisterRequest("student1", "student1@test.com", "pass1234!", Role.STUDENT, "20231001");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentNumber").value("20231001"));
    }

    @Test
    @DisplayName("POST /register — ADMIN role → 401 Unauthorized")
    void register_adminRole_shouldReturn401() throws Exception {
        RegisterRequest body = new RegisterRequest("admin2", "admin2@test.com", "pass1234!", Role.ADMIN, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("ADMIN")));
    }

    @Test
    @DisplayName("POST /register — duplicate email → 409 Conflict")
    void register_duplicateEmail_shouldReturn409() throws Exception {
        RegisterRequest first  = new RegisterRequest("user1", "dup@test.com", "pass1234!", Role.TEACHER, null);
        RegisterRequest second = new RegisterRequest("user2", "dup@test.com", "pass1234!", Role.TEACHER, null);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first))).andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("dup@test.com")));
    }

    @Test
    @DisplayName("POST /register — duplicate username → 409 Conflict")
    void register_duplicateUsername_shouldReturn409() throws Exception {
        RegisterRequest first  = new RegisterRequest("sameName", "a@test.com", "pass1234!", Role.TEACHER, null);
        RegisterRequest second = new RegisterRequest("sameName", "b@test.com", "pass1234!", Role.TEACHER, null);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first))).andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("sameName")));
    }

    @Test
    @DisplayName("POST /register — missing required fields → 400 Bad Request")
    void register_missingFields_shouldReturn400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register — password shorter than 8 chars → 400 Bad Request")
    void register_shortPassword_shouldReturn400() throws Exception {
        RegisterRequest body = new RegisterRequest("user3", "user3@test.com", "short", Role.TEACHER, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login — valid email identifier → 200 with tokens")
    void login_withEmail_shouldReturn200() throws Exception {
        registerUser("loginUser", "login@test.com", "pass1234!", Role.TEACHER);

        LoginRequest login = new LoginRequest("login@test.com", "pass1234!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @DisplayName("POST /login — valid username identifier → 200 with tokens")
    void login_withUsername_shouldReturn200() throws Exception {
        registerUser("loginUser2", "login2@test.com", "pass1234!", Role.STUDENT, "20231002");

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
        registerUser("loginUser3", "login3@test.com", "correctPass1!", Role.TEACHER);

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
        // After rotation the old token must be invalidated
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("POST /refresh — expired token in DB → 401 Unauthorized")
    void refresh_expiredToken_shouldReturn401() throws Exception {
        String refreshToken = loginAndGetRefreshToken("expiredUser", "expired@test.com", Role.TEACHER);

        // Force-expire the token in the database
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

    private void registerUser(String username, String email, String password, Role role) throws Exception {
        registerUser(username, email, password, role, null);
    }

    private void registerUser(String username, String email, String password, Role role, String studentNumber) throws Exception {
        RegisterRequest body = new RegisterRequest(username, email, password, role, studentNumber);
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken(String username, String email, Role role) throws Exception {
        registerUser(username, email, "pass1234!", role);
        LoginRequest login = new LoginRequest(email, "pass1234!");

        String responseJson = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).get("refreshToken").asText();
    }
}
