package tr.com.hacettepe.tams.rule_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.rule_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.rule_service.domain.Category;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourse;
import tr.com.hacettepe.tams.rule_service.dto.CategoryCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.CreateCategoryRequest;
import tr.com.hacettepe.tams.rule_service.dto.CreatePrefixLimitRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCategoryRequest;
import tr.com.hacettepe.tams.rule_service.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CategoryController}.
 * Exercises category CRUD scoped to a department plus the category course-pool
 * assignment endpoints (including the rule that a course must belong to the
 * department's pool before it can join a category), end-to-end against PostgreSQL.
 */
class CategoryControllerIT extends AbstractIntegrationTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DepartmentCourseRepository departmentCourseRepository;
    @Autowired private CategoryCourseRepository categoryCourseRepository;

    private String adminToken;
    private Department department;

    @BeforeEach
    void setUp() {
        categoryCourseRepository.deleteAll();
        departmentCourseRepository.deleteAll();
        categoryRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        adminToken = bearerToken("ADMIN");
        department = departmentRepository.save(new Department("Bilgisayar Mühendisliği", "BBM", null));
    }

    private String categoriesUrl() {
        return "/api/v1/departments/" + department.getId() + "/categories";
    }

    private Category persistCategory(String name) {
        return categoryRepository.save(new Category(
                department, name, null, new BigDecimal("12.00"), new BigDecimal("18.00"), 4));
    }

    private Course persistCourseInPool(String code) {
        Course course = courseRepository.save(
                new Course(code, "Course " + code, new BigDecimal("3.00"), new BigDecimal("5.00")));
        departmentCourseRepository.save(new DepartmentCourse(department, course));
        return course;
    }

    @Test
    @DisplayName("POST — 201 Created under a department")
    void create_validRequest_returns201() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Teknik Seçmeli", "electives", new BigDecimal("12.00"), new BigDecimal("18.00"), 4, null, null, null, null, null, null);

        mockMvc.perform(post(categoriesUrl())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/categories/")))
                .andExpect(jsonPath("$.name").value("Teknik Seçmeli"))
                .andExpect(jsonPath("$.departmentId").value(department.getId().toString()))
                .andExpect(jsonPath("$.minCourseCount").value(4));
    }

    @Test
    @DisplayName("POST — returns conditional fields and prefix limits in response")
    void create_withConditionalFieldsAndPrefixLimits_returnsAllFields() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Teknik Seçmeli Lab", "lab electives",
                new BigDecimal("12.00"), new BigDecimal("18.00"), 5,
                2015, 2025, List.of("BBM384"), 4, new BigDecimal("10.00"),
                List.of(new CreatePrefixLimitRequest("SEC", 3)));

        mockMvc.perform(post(categoriesUrl())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appliesFromYear").value(2015))
                .andExpect(jsonPath("$.appliesToYear").value(2025))
                .andExpect(jsonPath("$.conditionCourseCodes[0]").value("BBM384"))
                .andExpect(jsonPath("$.minCourseCountIfMet").value(4))
                .andExpect(jsonPath("$.minEctsIfMet").value(10.0))
                .andExpect(jsonPath("$.prefixLimits", hasSize(1)))
                .andExpect(jsonPath("$.prefixLimits[0].courseCodePrefix").value("SEC"))
                .andExpect(jsonPath("$.prefixLimits[0].maxCount").value(3));
    }

    @Test
    @DisplayName("POST — 400 when minCredit is negative")
    void create_negativeMinCredit_returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Bad", null, new BigDecimal("-1.00"), new BigDecimal("18.00"), 4, null, null, null, null, null, null);

        mockMvc.perform(post(categoriesUrl())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST — 401 without a token")
    void create_noToken_returns401() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Zorunlu", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null);

        mockMvc.perform(post(categoriesUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST — 403 with a TEACHER token")
    void create_teacherToken_returns403() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Zorunlu", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null);

        mockMvc.perform(post(categoriesUrl())
                        .header("Authorization", "Bearer " + bearerToken("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST — 409 on duplicate name within the same department")
    void create_duplicateName_returns409() throws Exception {
        persistCategory("Teknik Seçmeli");
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Teknik Seçmeli", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null);

        mockMvc.perform(post(categoriesUrl())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST — 404 when the department does not exist")
    void create_unknownDepartment_returns404() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Zorunlu", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/departments/" + UNKNOWN_ID + "/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET list — returns the department's categories")
    void list_returnsAll() throws Exception {
        persistCategory("Teknik Seçmeli");
        persistCategory("Bölüm Zorunlu");

        mockMvc.perform(get(categoriesUrl()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET by id — returns the category")
    void getById_returnsCategory() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");

        mockMvc.perform(get(categoriesUrl() + "/" + cat.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cat.getId().toString()))
                .andExpect(jsonPath("$.name").value("Teknik Seçmeli"));
    }

    @Test
    @DisplayName("GET by id — 404 for unknown category")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(categoriesUrl() + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT — updates category thresholds")
    void update_returnsUpdated() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Teknik Seçmeli v2", "updated", new BigDecimal("20.00"), new BigDecimal("30.00"), 6, null, null, null, null, null, null);

        mockMvc.perform(put(categoriesUrl() + "/" + cat.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Teknik Seçmeli v2"))
                .andExpect(jsonPath("$.minCourseCount").value(6));
    }

    @Test
    @DisplayName("PUT — replaces prefix limits on update")
    void update_replacesPrefixLimits() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Teknik Seçmeli", "updated", new BigDecimal("12.00"), new BigDecimal("18.00"), 4,
                null, null, null, null, null,
                List.of(new CreatePrefixLimitRequest("BBM", 2)));

        mockMvc.perform(put(categoriesUrl() + "/" + cat.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefixLimits", hasSize(1)))
                .andExpect(jsonPath("$.prefixLimits[0].courseCodePrefix").value("BBM"))
                .andExpect(jsonPath("$.prefixLimits[0].maxCount").value(2));
    }

    @Test
    @DisplayName("PUT — 404 for unknown category")
    void update_notFound_returns404() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "X", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null);

        mockMvc.perform(put(categoriesUrl() + "/" + UNKNOWN_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE — 204 then 404 on re-fetch")
    void delete_removesCategory() throws Exception {
        Category cat = persistCategory("Silinecek");

        mockMvc.perform(delete(categoriesUrl() + "/" + cat.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(categoriesUrl() + "/" + cat.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Category courses — add, list, then remove a pooled course")
    void categoryCourses_addListRemove() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        Course course = persistCourseInPool("BIL401");
        String url = "/api/v1/categories/" + cat.getId() + "/courses";
        CategoryCourseRequest request = new CategoryCourseRequest(course.getId(), true, null, null);

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("BIL401"))
                .andExpect(jsonPath("$.isMandatory").value(true));

        mockMvc.perform(get(url).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("BIL401"));

        mockMvc.perform(delete(url + "/" + course.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(url).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Category courses — 409 when the course is not in the department pool")
    void categoryCourses_courseNotInPool_returns409() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        Course course = courseRepository.save(
                new Course("BIL402", "Outside Pool", new BigDecimal("3.00"), new BigDecimal("5.00")));
        CategoryCourseRequest request = new CategoryCourseRequest(course.getId(), false, null, null);

        mockMvc.perform(post("/api/v1/categories/" + cat.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Category courses — 409 when the course is already assigned")
    void categoryCourses_duplicateAssignment_returns409() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        Course course = persistCourseInPool("BIL403");
        String url = "/api/v1/categories/" + cat.getId() + "/courses";
        CategoryCourseRequest request = new CategoryCourseRequest(course.getId(), false, null, null);

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Category courses — 404 when adding an unknown course")
    void categoryCourses_unknownCourse_returns404() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        CategoryCourseRequest request = new CategoryCourseRequest(UUID.fromString(UNKNOWN_ID), false, null, null);

        mockMvc.perform(post("/api/v1/categories/" + cat.getId() + "/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Category courses — 404 when removing a course not assigned")
    void categoryCourses_removeMissing_returns404() throws Exception {
        Category cat = persistCategory("Teknik Seçmeli");
        Course course = persistCourseInPool("BIL404");

        mockMvc.perform(delete("/api/v1/categories/" + cat.getId() + "/courses/" + course.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
