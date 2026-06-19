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
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.dto.CreateDepartmentRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateDepartmentRequest;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link DepartmentController}.
 * Runs against a real PostgreSQL container with the production Spring Security
 * chain active, so authentication, authorization, validation, and persistence
 * are all exercised end-to-end. Covers full CRUD plus the course-pool endpoints.
 */
class DepartmentControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/departments";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DepartmentCourseRepository departmentCourseRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        departmentCourseRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        adminToken = bearerToken("ADMIN");
    }

    @Test
    @DisplayName("POST — 201 Created with valid ADMIN token")
    void create_validRequest_returns201() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Bilgisayar Mühendisliği", "BBM", "CS");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/departments/")))
                .andExpect(jsonPath("$.name").value("Bilgisayar Mühendisliği"))
                .andExpect(jsonPath("$.code").value("BBM"))
                .andExpect(jsonPath("$.description").value("CS"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST — 400 when name is blank")
    void create_blankName_returns400() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("  ", "XX", null);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST — 401 without a token")
    void create_noToken_returns401() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Test", "TST", null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST — 403 with a TEACHER token")
    void create_teacherToken_returns403() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Test", "TST", null);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + bearerToken("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST — 409 on duplicate name")
    void create_duplicateName_returns409() throws Exception {
        departmentRepository.save(new Department("Bilgisayar Mühendisliği", "BBM", null));
        CreateDepartmentRequest request = new CreateDepartmentRequest("Bilgisayar Mühendisliği", "BBM", null);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET list — returns persisted departments")
    void list_returnsAll() throws Exception {
        departmentRepository.save(new Department("Elektrik Mühendisliği", "EE", null));

        mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Elektrik Mühendisliği"));
    }

    @Test
    @DisplayName("GET by id — returns the department")
    void getById_returnsDepartment() throws Exception {
        Department saved = departmentRepository.save(new Department("Makine Mühendisliği", "ME", null));

        mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Makine Mühendisliği"));
    }

    @Test
    @DisplayName("GET by id — 404 for unknown id")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT — updates name and description")
    void update_returnsUpdated() throws Exception {
        Department saved = departmentRepository.save(new Department("Eski Ad", "EA", null));
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("Yeni Ad", "YA", "new");

        mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Ad"))
                .andExpect(jsonPath("$.description").value("new"));
    }

    @Test
    @DisplayName("PUT — 404 for unknown id")
    void update_notFound_returns404() throws Exception {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("X", "XX", null);

        mockMvc.perform(put(BASE_URL + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE — 204 then 404 on re-fetch")
    void delete_removesDepartment() throws Exception {
        Department saved = departmentRepository.save(new Department("Silinecek", "SIL", null));

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

    @Test
    @DisplayName("Course pool — add, list, then remove a course")
    void coursePool_addListRemove() throws Exception {
        Department dept = departmentRepository.save(new Department("Bilgisayar", "BLG", null));
        Course course = courseRepository.save(
                new Course("BIL101", "Intro to CS", new BigDecimal("4.00"), new BigDecimal("6.00")));

        mockMvc.perform(post(BASE_URL + "/" + dept.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + dept.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("BIL101"));

        mockMvc.perform(delete(BASE_URL + "/" + dept.getId() + "/courses/" + course.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + dept.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Course pool — adding the same course twice returns 409")
    void coursePool_addDuplicate_returns409() throws Exception {
        Department dept = departmentRepository.save(new Department("Bilgisayar", "BLG", null));
        Course course = courseRepository.save(
                new Course("BIL102", "Data Structures", new BigDecimal("4.00"), new BigDecimal("6.00")));
        String url = BASE_URL + "/" + dept.getId() + "/courses";

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Course pool — adding an unknown course returns 404")
    void coursePool_unknownCourse_returns404() throws Exception {
        Department dept = departmentRepository.save(new Department("Bilgisayar", "BLG", null));

        mockMvc.perform(post(BASE_URL + "/" + dept.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("courseId", UNKNOWN_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Course pool — removing a course not in the pool returns 404")
    void coursePool_removeMissing_returns404() throws Exception {
        Department dept = departmentRepository.save(new Department("Bilgisayar", "BLG", null));
        Course course = courseRepository.save(
                new Course("BIL103", "Algorithms", new BigDecimal("4.00"), new BigDecimal("6.00")));

        mockMvc.perform(delete(BASE_URL + "/" + dept.getId() + "/courses/" + course.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
