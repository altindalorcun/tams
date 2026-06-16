package tr.com.hacettepe.tams.rule_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.rule_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.dto.CreateCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCourseRequest;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CourseController} (global course catalog).
 * Covers CRUD, authorization, validation, and the unique-code constraint
 * end-to-end against a real PostgreSQL container.
 */
class CourseControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/courses";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DepartmentCourseRepository departmentCourseRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        departmentCourseRepository.deleteAll();
        courseRepository.deleteAll();
        adminToken = bearerToken("ADMIN");
    }

    @Test
    @DisplayName("POST — 201 Created with valid ADMIN token")
    void create_validRequest_returns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest(
                "MAT101", "Calculus I", new BigDecimal("4.00"), new BigDecimal("5.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/courses/")))
                .andExpect(jsonPath("$.courseCode").value("MAT101"))
                .andExpect(jsonPath("$.courseName").value("Calculus I"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST — 400 when credits is not positive")
    void create_nonPositiveCredit_returns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest(
                "MAT101", "Calculus I", new BigDecimal("0.00"), new BigDecimal("5.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST — 401 without a token")
    void create_noToken_returns401() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest(
                "MAT101", "Calculus I", new BigDecimal("4.00"), new BigDecimal("5.00"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST — 403 with a STUDENT token")
    void create_studentToken_returns403() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest(
                "MAT101", "Calculus I", new BigDecimal("4.00"), new BigDecimal("5.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + bearerToken("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST — 409 on duplicate course code")
    void create_duplicateCode_returns409() throws Exception {
        courseRepository.save(new Course("MAT101", "Calculus I",
                new BigDecimal("4.00"), new BigDecimal("5.00")));
        CreateCourseRequest request = new CreateCourseRequest(
                "MAT101", "Different Name", new BigDecimal("3.00"), new BigDecimal("4.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET list — returns persisted courses")
    void list_returnsAll() throws Exception {
        courseRepository.save(new Course("FIZ101", "Physics I",
                new BigDecimal("3.00"), new BigDecimal("4.00")));

        mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("FIZ101"));
    }

    @Test
    @DisplayName("GET by id — returns the course")
    void getById_returnsCourse() throws Exception {
        Course saved = courseRepository.save(new Course("KIM101", "Chemistry",
                new BigDecimal("3.00"), new BigDecimal("5.00")));

        mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("KIM101"));
    }

    @Test
    @DisplayName("GET by id — 404 for unknown id")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT — updates course fields")
    void update_returnsUpdated() throws Exception {
        Course saved = courseRepository.save(new Course("MAT101", "Calculus I",
                new BigDecimal("4.00"), new BigDecimal("5.00")));
        UpdateCourseRequest request = new UpdateCourseRequest(
                "MAT102", "Calculus II", new BigDecimal("3.00"), new BigDecimal("4.00"));

        mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("MAT102"))
                .andExpect(jsonPath("$.courseName").value("Calculus II"));
    }

    @Test
    @DisplayName("PUT — 404 for unknown id")
    void update_notFound_returns404() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest(
                "X101", "X", new BigDecimal("1.00"), new BigDecimal("1.00"));

        mockMvc.perform(put(BASE_URL + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE — 204 then 404 on re-fetch")
    void delete_removesCourse() throws Exception {
        Course saved = courseRepository.save(new Course("DEL101", "Delete Me",
                new BigDecimal("2.00"), new BigDecimal("3.00")));

        mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE — 404 for unknown id")
    void delete_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
