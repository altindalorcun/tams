package tr.com.hacettepe.tams.rule_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourseId;
import tr.com.hacettepe.tams.rule_service.dto.CourseResponse;
import tr.com.hacettepe.tams.rule_service.dto.CreateCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCourseRequest;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CourseService}.
 * Repository is mocked; covers the global course catalog CRUD happy paths and
 * every failure branch (duplicate code, not found).
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private DepartmentCourseRepository departmentCourseRepository;
    @InjectMocks private CourseService courseService;

    private Course course;
    private final UUID COURSE_ID = UUID.randomUUID();
    private final UUID DEPT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        course = new Course("MAT101", "Calculus I", new BigDecimal("4.00"), new BigDecimal("5.00"));
        course.setId(COURSE_ID);
    }

    @Test
    @DisplayName("create: happy path — persists and returns response")
    void create_happyPath() {
        when(courseRepository.existsByCourseCode("MAT101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        CourseResponse result = courseService.create(
                new CreateCourseRequest("MAT101", "Calculus I",
                        new BigDecimal("4.00"), new BigDecimal("5.00")));

        assertThat(result.courseCode()).isEqualTo("MAT101");
        assertThat(result.courseName()).isEqualTo("Calculus I");
        assertThat(result.credit()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(result.ects()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.departmentIds()).isEmpty();
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("create: duplicate course code — throws DuplicateResourceException")
    void create_duplicateCode_throws() {
        when(courseRepository.existsByCourseCode("MAT101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(
                new CreateCourseRequest("MAT101", "Calculus I",
                        new BigDecimal("4.00"), new BigDecimal("5.00"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: returns mapped list with departmentIds")
    void findAll_returnsList() {
        Course other = new Course("FIZ101", "Physics I", new BigDecimal("3.00"), new BigDecimal("4.00"));
        when(courseRepository.findAll()).thenReturn(List.of(course, other));
        when(departmentCourseRepository.findAllIds()).thenReturn(
                List.of(new DepartmentCourseId(DEPT_ID, course.getId())));

        List<CourseResponse> results = courseService.findAll();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(CourseResponse::courseCode).containsExactly("MAT101", "FIZ101");
        assertThat(results.get(0).departmentIds()).containsExactly(DEPT_ID);
        assertThat(results.get(1).departmentIds()).isEmpty();
    }

    @Test
    @DisplayName("findAll: empty repository returns empty list")
    void findAll_empty_returnsEmpty() {
        when(courseRepository.findAll()).thenReturn(List.of());
        when(departmentCourseRepository.findAllIds()).thenReturn(List.of());

        assertThat(courseService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findById: happy path returns response with departmentIds")
    void findById_happyPath() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(departmentCourseRepository.findDepartmentIdsByCourseId(COURSE_ID)).thenReturn(List.of(DEPT_ID));

        CourseResponse result = courseService.findById(COURSE_ID);

        assertThat(result.courseCode()).isEqualTo("MAT101");
        assertThat(result.departmentIds()).containsExactly(DEPT_ID);
    }

    @Test
    @DisplayName("findById: not found — throws ResourceNotFoundException")
    void findById_notFound_throws() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: changes all fields and saves")
    void update_happyPath() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseRepository.existsByCourseCode("MAT102")).thenReturn(false);
        when(courseRepository.save(course)).thenReturn(course);
        when(departmentCourseRepository.findDepartmentIdsByCourseId(COURSE_ID)).thenReturn(List.of(DEPT_ID));

        CourseResponse result = courseService.update(COURSE_ID,
                new UpdateCourseRequest("MAT102", "Calculus II",
                        new BigDecimal("3.00"), new BigDecimal("4.00")));

        assertThat(course.getCourseCode()).isEqualTo("MAT102");
        assertThat(course.getCourseName()).isEqualTo("Calculus II");
        assertThat(course.getCredit()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(course.getEcts()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(result.departmentIds()).containsExactly(DEPT_ID);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("update: keeping the same code skips the duplicate check")
    void update_sameCode_skipsDuplicateCheck() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        when(departmentCourseRepository.findDepartmentIdsByCourseId(COURSE_ID)).thenReturn(List.of());

        courseService.update(COURSE_ID,
                new UpdateCourseRequest("MAT101", "Calculus I (revised)",
                        new BigDecimal("4.00"), new BigDecimal("6.00")));

        verify(courseRepository, never()).existsByCourseCode(any());
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("update: renaming code to a taken value — throws DuplicateResourceException")
    void update_renameToTakenCode_throws() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseRepository.existsByCourseCode("MAT102")).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(COURSE_ID,
                new UpdateCourseRequest("MAT102", "Calculus II",
                        new BigDecimal("3.00"), new BigDecimal("4.00"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: not found — throws ResourceNotFoundException")
    void update_notFound_throws() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(COURSE_ID,
                new UpdateCourseRequest("MAT102", "Calculus II",
                        new BigDecimal("3.00"), new BigDecimal("4.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: happy path — removes by id")
    void delete_happyPath() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);

        courseService.delete(COURSE_ID);

        verify(courseRepository).deleteById(COURSE_ID);
    }

    @Test
    @DisplayName("delete: not found — throws ResourceNotFoundException")
    void delete_notFound_throws() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> courseService.delete(COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courseRepository, never()).deleteById(any());
    }
}
