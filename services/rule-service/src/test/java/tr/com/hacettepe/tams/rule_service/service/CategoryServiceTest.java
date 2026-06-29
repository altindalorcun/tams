package tr.com.hacettepe.tams.rule_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.hacettepe.tams.rule_service.domain.Category;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.dto.*;
import tr.com.hacettepe.tams.rule_service.exception.ConflictException;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.*;
import tr.com.hacettepe.tams.rule_service.repository.CurriculumEquivalenceRuleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CategoryService}.
 * Repositories are mocked. Domain objects are given explicit UUIDs so the
 * department-pool consistency rule and rule-set aggregation can be exercised precisely.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryCourseRepository categoryCourseRepository;
    @Mock private CategoryPrefixLimitRepository categoryPrefixLimitRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private DepartmentCourseRepository departmentCourseRepository;
    @Mock private CurriculumEquivalenceRuleRepository curriculumEquivalenceRuleRepository;

    @InjectMocks private CategoryService categoryService;

    private Department department;
    private Category category;
    private Course course;
    private final UUID DEPT_ID = UUID.randomUUID();
    private final UUID CAT_ID = UUID.randomUUID();
    private final UUID COURSE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        department = new Department("Bilgisayar Mühendisliği", "BBM", null);
        department.setId(DEPT_ID);
        category = new Category(department, "Teknik Seçmeli", null,
                BigDecimal.ZERO, BigDecimal.ZERO, 5);
        category.setId(CAT_ID);
        course = new Course("BIL401", "Machine Learning", new BigDecimal("3.00"), new BigDecimal("4.00"));
        course.setId(COURSE_ID);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path — persists category under department")
        void create_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.existsByDepartmentIdAndName(DEPT_ID, "Teknik Seçmeli")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            CategoryResponse result = categoryService.create(DEPT_ID,
                    new CreateCategoryRequest("Teknik Seçmeli", "desc",
                            new BigDecimal("12.00"), new BigDecimal("18.00"), 5, null, null, null, null, null, null));

            assertThat(result.name()).isEqualTo("Teknik Seçmeli");
            assertThat(result.minCourseCount()).isEqualTo(5);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("happy path — maps conditional fields and prefix limits in response")
        void create_withConditionalFieldsAndPrefixLimits() {
            category.setAppliesFromYear(2015);
            category.setAppliesToYear(2025);
            category.setConditionCourseCodes(new String[]{"BBM384"});
            category.setMinCourseCountIfMet(4);
            category.setMinEctsIfMet(new BigDecimal("10.00"));
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.existsByDepartmentIdAndName(DEPT_ID, "Teknik Seçmeli")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category saved = invocation.getArgument(0);
                saved.setId(CAT_ID);
                return saved;
            });

            CategoryResponse result = categoryService.create(DEPT_ID,
                    new CreateCategoryRequest("Teknik Seçmeli", "desc",
                            new BigDecimal("12.00"), new BigDecimal("18.00"), 5,
                            2015, 2025, List.of("BBM384"), 4, new BigDecimal("10.00"),
                            List.of(new CreatePrefixLimitRequest("SEC", 3))));

            assertThat(result.appliesFromYear()).isEqualTo(2015);
            assertThat(result.appliesToYear()).isEqualTo(2025);
            assertThat(result.conditionCourseCodes()).containsExactly("BBM384");
            assertThat(result.minCourseCountIfMet()).isEqualTo(4);
            assertThat(result.minEctsIfMet()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(result.prefixLimits()).hasSize(1);
            assertThat(result.prefixLimits().get(0).courseCodePrefix()).isEqualTo("SEC");
            assertThat(result.prefixLimits().get(0).maxCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("duplicate name in same department — throws DuplicateResourceException")
        void create_duplicateNameSameDept_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.existsByDepartmentIdAndName(DEPT_ID, "Teknik Seçmeli")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.create(DEPT_ID,
                    new CreateCategoryRequest("Teknik Seçmeli", null,
                            BigDecimal.ZERO, BigDecimal.ZERO, 5, null, null, null, null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("department not found — throws ResourceNotFoundException")
        void create_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.create(DEPT_ID,
                    new CreateCategoryRequest("Zorunlu", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("findByDepartment — returns categories of the department")
        void findByDepartment_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByDepartmentId(DEPT_ID)).thenReturn(List.of(category));

            List<CategoryResponse> result = categoryService.findByDepartment(DEPT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Teknik Seçmeli");
        }

        @Test
        @DisplayName("findByDepartment — department not found throws")
        void findByDepartment_notFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findByDepartment(DEPT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findById — returns category scoped to department")
        void findById_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));

            CategoryResponse result = categoryService.findById(DEPT_ID, CAT_ID);

            assertThat(result.id()).isEqualTo(CAT_ID);
        }

        @Test
        @DisplayName("findById — category missing in department throws")
        void findById_categoryNotInDept_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(DEPT_ID, CAT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findById — department not found throws")
        void findById_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(DEPT_ID, CAT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("happy path — applies all threshold changes")
        void update_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.update(DEPT_ID, CAT_ID,
                    new UpdateCategoryRequest("Teknik Seçmeli (v2)", "new desc",
                            new BigDecimal("20.00"), new BigDecimal("30.00"), 7, null, null, null, null, null, null));

            assertThat(category.getName()).isEqualTo("Teknik Seçmeli (v2)");
            assertThat(category.getMinCredit()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(category.getMinEcts()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(category.getMinCourseCount()).isEqualTo(7);
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("keeping the same name skips the duplicate check")
        void update_sameName_skipsDuplicateCheck() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.update(DEPT_ID, CAT_ID,
                    new UpdateCategoryRequest("Teknik Seçmeli", "desc",
                            BigDecimal.ZERO, BigDecimal.ZERO, 5, null, null, null, null, null, null));

            verify(categoryRepository, never()).existsByDepartmentIdAndName(any(), any());
        }

        @Test
        @DisplayName("update — replaces prefix limits")
        void update_replacesPrefixLimits() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.update(DEPT_ID, CAT_ID,
                    new UpdateCategoryRequest("Teknik Seçmeli", "desc",
                            BigDecimal.ZERO, BigDecimal.ZERO, 5, null, null, null, null, null,
                            List.of(new CreatePrefixLimitRequest("BBM", 2))));

            assertThat(category.getPrefixLimits()).hasSize(1);
            assertThat(category.getPrefixLimits().get(0).getCourseCodePrefix()).isEqualTo("BBM");
            assertThat(category.getPrefixLimits().get(0).getMaxCount()).isEqualTo(2);
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("rename to a name taken in the department — throws DuplicateResourceException")
        void update_renameToTaken_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByDepartmentIdAndName(DEPT_ID, "Zorunlu")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.update(DEPT_ID, CAT_ID,
                    new UpdateCategoryRequest("Zorunlu", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("category not found — throws ResourceNotFoundException")
        void update_categoryNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(DEPT_ID, CAT_ID,
                    new UpdateCategoryRequest("X", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("happy path — deletes the category")
        void delete_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.of(category));

            categoryService.delete(DEPT_ID, CAT_ID);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("category not found — throws ResourceNotFoundException")
        void delete_notFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByIdAndDepartmentId(CAT_ID, DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(DEPT_ID, CAT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("category course assignment")
    class CourseAssignment {

        @Test
        @DisplayName("addCourse — happy path persists junction with mandatory flag")
        void addCourse_happyPath() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(true);
            when(categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(CAT_ID, COURSE_ID)).thenReturn(false);
            when(categoryCourseRepository.save(any(CategoryCourse.class))).thenAnswer(i -> i.getArguments()[0]);

            CategoryCourseResponse result = categoryService.addCourse(CAT_ID,
                    new CategoryCourseRequest(COURSE_ID, true, null, null, null, null));

            assertThat(result.courseCode()).isEqualTo("BIL401");
            assertThat(result.isMandatory()).isTrue();
            verify(categoryCourseRepository).save(any(CategoryCourse.class));
        }

        @Test
        @DisplayName("addCourse — category not found throws")
        void addCourse_categoryNotFound_throws() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.addCourse(CAT_ID,
                    new CategoryCourseRequest(COURSE_ID, false, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("addCourse — course not found throws")
        void addCourse_courseNotFound_throws() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.addCourse(CAT_ID,
                    new CategoryCourseRequest(COURSE_ID, false, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("addCourse — course not in department pool throws ConflictException")
        void addCourse_courseNotInDepartmentPool_throws() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.addCourse(CAT_ID,
                    new CategoryCourseRequest(COURSE_ID, false, null, null, null, null)))
                    .isInstanceOf(ConflictException.class);
            verify(categoryCourseRepository, never()).save(any());
        }

        @Test
        @DisplayName("addCourse — course already in category throws DuplicateResourceException")
        void addCourse_alreadyInCategory_throws() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(true);
            when(categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(CAT_ID, COURSE_ID)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.addCourse(CAT_ID,
                    new CategoryCourseRequest(COURSE_ID, true, null, null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(categoryCourseRepository, never()).save(any());
        }

        @Test
        @DisplayName("findCourses — returns mapped course assignments")
        void findCourses_happyPath() {
            CategoryCourse cc = new CategoryCourse(category, course, true);
            when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
            when(categoryCourseRepository.findByCategoryIdWithCourse(CAT_ID)).thenReturn(List.of(cc));

            List<CategoryCourseResponse> result = categoryService.findCourses(CAT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).courseCode()).isEqualTo("BIL401");
            assertThat(result.get(0).isMandatory()).isTrue();
        }

        @Test
        @DisplayName("findCourses — category not found throws")
        void findCourses_categoryNotFound_throws() {
            when(categoryRepository.existsById(CAT_ID)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.findCourses(CAT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findCoursePool — splits department pool into assigned and available, sorted by course code")
        void findCoursePool_splitsAndSorts() {
            Course courseB = new Course("BIL402", "Deep Learning", new BigDecimal("3.00"), new BigDecimal("4.00"));
            courseB.setId(UUID.randomUUID());
            Course courseC = new Course("BIL403", "Computer Vision", new BigDecimal("3.00"), new BigDecimal("4.00"));
            courseC.setId(UUID.randomUUID());

            CategoryCourse assignedA = new CategoryCourse(category, course, true);
            CategoryCourse assignedC = new CategoryCourse(category, courseC, false);

            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category));
            when(categoryCourseRepository.findByCategoryIdWithCourse(CAT_ID))
                    .thenReturn(List.of(assignedC, assignedA));
            when(departmentCourseRepository.findCoursesByDepartmentId(DEPT_ID))
                    .thenReturn(List.of(courseC, course, courseB));

            CategoryCoursePoolResponse result = categoryService.findCoursePool(CAT_ID);

            assertThat(result.assignedCourses()).extracting(CategoryCourseResponse::courseCode)
                    .containsExactly("BIL401", "BIL403");
            assertThat(result.availableCourses()).extracting(DepartmentCourseItem::courseCode)
                    .containsExactly("BIL402");
            assertThat(result.assignedCourses()).noneMatch(c -> c.courseCode().equals("BIL402"));
            assertThat(result.availableCourses()).noneMatch(c -> c.courseCode().equals("BIL401"));
        }

        @Test
        @DisplayName("findCoursePool — category not found throws")
        void findCoursePool_categoryNotFound_throws() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findCoursePool(CAT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("removeCourse — happy path deletes the assignment")
        void removeCourse_happyPath() {
            when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
            when(categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(CAT_ID, COURSE_ID)).thenReturn(true);

            categoryService.removeCourse(CAT_ID, COURSE_ID);

            verify(categoryCourseRepository).deleteByIdCategoryIdAndIdCourseId(CAT_ID, COURSE_ID);
        }

        @Test
        @DisplayName("removeCourse — category not found throws")
        void removeCourse_categoryNotFound_throws() {
            when(categoryRepository.existsById(CAT_ID)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.removeCourse(CAT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryCourseRepository, never()).deleteByIdCategoryIdAndIdCourseId(any(), any());
        }

        @Test
        @DisplayName("removeCourse — course not assigned throws")
        void removeCourse_notAssigned_throws() {
            when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
            when(categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(CAT_ID, COURSE_ID)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.removeCourse(CAT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryCourseRepository, never()).deleteByIdCategoryIdAndIdCourseId(any(), any());
        }
    }

    @Nested
    @DisplayName("getRuleSet (internal)")
    class GetRuleSet {

        @Test
        @DisplayName("aggregates categories and their courses for a department")
        void getRuleSet_aggregatesCorrectly() {
            category.getCategoryCourses().add(new CategoryCourse(category, course, true));
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(categoryRepository.findByDepartmentIdWithCourses(DEPT_ID)).thenReturn(List.of(category));
            when(curriculumEquivalenceRuleRepository.findByDepartmentId(DEPT_ID)).thenReturn(List.of());

            RuleSetResponse result = categoryService.getRuleSet(DEPT_ID);

            assertThat(result.departmentId()).isEqualTo(DEPT_ID);
            assertThat(result.departmentName()).isEqualTo("Bilgisayar Mühendisliği");
            assertThat(result.categories()).hasSize(1);
            RuleCategoryDto cat = result.categories().get(0);
            assertThat(cat.minCourseCount()).isEqualTo(5);
            assertThat(cat.courses()).hasSize(1);
            assertThat(cat.courses().get(0).courseCode()).isEqualTo("BIL401");
            assertThat(cat.courses().get(0).isMandatory()).isTrue();
        }

        @Test
        @DisplayName("department not found — throws ResourceNotFoundException")
        void getRuleSet_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getRuleSet(DEPT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
