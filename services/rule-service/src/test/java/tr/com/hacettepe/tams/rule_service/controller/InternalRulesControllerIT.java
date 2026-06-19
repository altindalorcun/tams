package tr.com.hacettepe.tams.rule_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.rule_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.rule_service.domain.Category;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourse;
import tr.com.hacettepe.tams.rule_service.repository.*;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link InternalRulesController}.
 * The {@code /internal/rules/**} endpoint is consumed by analysis-service and
 * must be reachable without authentication while still returning the fully
 * aggregated rule set (categories with their courses and mandatory flags).
 */
class InternalRulesControllerIT extends AbstractIntegrationTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DepartmentCourseRepository departmentCourseRepository;
    @Autowired private CategoryCourseRepository categoryCourseRepository;

    @BeforeEach
    void setUp() {
        categoryCourseRepository.deleteAll();
        departmentCourseRepository.deleteAll();
        categoryRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /internal/rules/{id} — no auth required, returns aggregated rule set")
    void getRuleSet_returnsAggregatedRules() throws Exception {
        Department dept = departmentRepository.save(new Department("Bilgisayar Mühendisliği", "BBM", null));
        Course course = courseRepository.save(
                new Course("BIL401", "Machine Learning", new BigDecimal("3.00"), new BigDecimal("4.00")));
        departmentCourseRepository.save(new DepartmentCourse(dept, course));
        Category category = categoryRepository.save(new Category(
                dept, "Teknik Seçmeli", null, new BigDecimal("12.00"), new BigDecimal("18.00"), 4));
        categoryCourseRepository.save(new CategoryCourse(category, course, true));

        mockMvc.perform(get("/internal/rules/" + dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(dept.getId().toString()))
                .andExpect(jsonPath("$.departmentName").value("Bilgisayar Mühendisliği"))
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].name").value("Teknik Seçmeli"))
                .andExpect(jsonPath("$.categories[0].minCourseCount").value(4))
                .andExpect(jsonPath("$.categories[0].courses", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].courses[0].courseCode").value("BIL401"))
                .andExpect(jsonPath("$.categories[0].courses[0].isMandatory").value(true));
    }

    @Test
    @DisplayName("GET /internal/rules/{id} — empty categories for a department without rules")
    void getRuleSet_noCategories_returnsEmptyList() throws Exception {
        Department dept = departmentRepository.save(new Department("Yeni Bölüm", "YB", null));

        mockMvc.perform(get("/internal/rules/" + dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    @Test
    @DisplayName("GET /internal/rules/{id} — 404 for unknown department")
    void getRuleSet_unknownDepartment_returns404() throws Exception {
        mockMvc.perform(get("/internal/rules/" + UNKNOWN_ID))
                .andExpect(status().isNotFound());
    }
}
