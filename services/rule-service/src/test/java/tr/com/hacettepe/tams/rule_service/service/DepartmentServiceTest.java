package tr.com.hacettepe.tams.rule_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourse;
import tr.com.hacettepe.tams.rule_service.dto.CourseResponse;
import tr.com.hacettepe.tams.rule_service.dto.CreateDepartmentRequest;
import tr.com.hacettepe.tams.rule_service.dto.DepartmentResponse;
import tr.com.hacettepe.tams.rule_service.dto.UpdateDepartmentRequest;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DepartmentService}.
 * All repository dependencies are mocked — no Spring context or database is involved.
 * Covers every public method on its happy path plus each distinct failure branch.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private DepartmentCourseRepository departmentCourseRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private Course course;
    private final UUID DEPT_ID = UUID.randomUUID();
    private final UUID COURSE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        department = new Department("Bilgisayar Mühendisliği", "BBM", "CS department");
        course = new Course("MAT101", "Calculus I", new BigDecimal("4.00"), new BigDecimal("5.00"));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path — saves and returns response")
        void create_happyPath() {
            when(departmentRepository.existsByName("Bilgisayar Mühendisliği")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenReturn(department);

            DepartmentResponse result = departmentService.create(
                    new CreateDepartmentRequest("Bilgisayar Mühendisliği", "BBM", "CS department", null, null));

            assertThat(result.name()).isEqualTo("Bilgisayar Mühendisliği");
            assertThat(result.description()).isEqualTo("CS department");

            ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
            verify(departmentRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Bilgisayar Mühendisliği");
        }

        @Test
        @DisplayName("duplicate name — throws DuplicateResourceException and never saves")
        void create_duplicateName_throws() {
            when(departmentRepository.existsByName("Bilgisayar Mühendisliği")).thenReturn(true);

            assertThatThrownBy(() -> departmentService.create(
                    new CreateDepartmentRequest("Bilgisayar Mühendisliği", "BBM", null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("findAll — maps every department to a response")
        void findAll_returnsMappedList() {
            Department other = new Department("Elektrik", "EE", null);
            when(departmentRepository.findAllSortedByNameAsc()).thenReturn(List.of(department, other));

            List<DepartmentResponse> result = departmentService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(DepartmentResponse::name)
                    .containsExactly("Bilgisayar Mühendisliği", "Elektrik");
        }

        @Test
        @DisplayName("findAll — empty repository returns empty list")
        void findAll_empty_returnsEmpty() {
            when(departmentRepository.findAllSortedByNameAsc()).thenReturn(List.of());

            assertThat(departmentService.findAll()).isEmpty();
        }

        @Test
        @DisplayName("findAll — returns departments in repository sort order")
        void findAll_preservesRepositorySortOrder() {
            Department zeta = new Department("Zeta Bölüm", "ZET", null);
            Department alpha = new Department("Alpha Bölüm", "ALP", null);
            when(departmentRepository.findAllSortedByNameAsc()).thenReturn(List.of(alpha, zeta));

            List<DepartmentResponse> result = departmentService.findAll();

            assertThat(result).extracting(DepartmentResponse::name)
                    .containsExactly("Alpha Bölüm", "Zeta Bölüm");
        }

        @Test
        @DisplayName("findById — happy path returns response")
        void findById_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));

            DepartmentResponse result = departmentService.findById(DEPT_ID);

            assertThat(result.name()).isEqualTo("Bilgisayar Mühendisliği");
        }

        @Test
        @DisplayName("findById — not found throws ResourceNotFoundException")
        void findById_notFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.findById(DEPT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("happy path — persists new name and description")
        void update_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.existsByName("Elektrik Mühendisliği")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(department);

            departmentService.update(DEPT_ID,
                    new UpdateDepartmentRequest("Elektrik Mühendisliği", "EE", "EE department", null, null));

            assertThat(department.getName()).isEqualTo("Elektrik Mühendisliği");
            assertThat(department.getDescription()).isEqualTo("EE department");
            verify(departmentRepository).save(department);
        }

        @Test
        @DisplayName("same name kept — skips the duplicate check and saves")
        void update_sameName_skipsDuplicateCheck() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.save(any())).thenReturn(department);

            departmentService.update(DEPT_ID,
                    new UpdateDepartmentRequest("Bilgisayar Mühendisliği", "BBM", "updated desc", null, null));

            verify(departmentRepository, never()).existsByName(any());
            verify(departmentRepository).save(department);
        }

        @Test
        @DisplayName("rename to an existing name — throws DuplicateResourceException")
        void update_renameToTaken_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.existsByName("Elektrik Mühendisliği")).thenReturn(true);

            assertThatThrownBy(() -> departmentService.update(DEPT_ID,
                    new UpdateDepartmentRequest("Elektrik Mühendisliği", "EE", null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("not found — throws ResourceNotFoundException")
        void update_notFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.update(DEPT_ID,
                    new UpdateDepartmentRequest("Anything", "ANY", null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("happy path — deletes by id")
        void delete_happyPath() {
            when(departmentRepository.existsById(DEPT_ID)).thenReturn(true);

            departmentService.delete(DEPT_ID);

            verify(departmentRepository).deleteById(DEPT_ID);
        }

        @Test
        @DisplayName("not found — throws and never deletes")
        void delete_notFound_throws() {
            when(departmentRepository.existsById(DEPT_ID)).thenReturn(false);

            assertThatThrownBy(() -> departmentService.delete(DEPT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(departmentRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("course pool")
    class CoursePool {

        @Test
        @DisplayName("addCourse — happy path saves junction row")
        void addCourse_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(false);
            when(departmentCourseRepository.save(any(DepartmentCourse.class))).thenAnswer(i -> i.getArguments()[0]);

            departmentService.addCourse(DEPT_ID, COURSE_ID);

            verify(departmentCourseRepository).save(any(DepartmentCourse.class));
        }

        @Test
        @DisplayName("addCourse — department not found throws")
        void addCourse_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.addCourse(DEPT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(departmentCourseRepository, never()).save(any());
        }

        @Test
        @DisplayName("addCourse — course not found throws")
        void addCourse_courseNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.addCourse(DEPT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(departmentCourseRepository, never()).save(any());
        }

        @Test
        @DisplayName("addCourse — already in pool throws DuplicateResourceException")
        void addCourse_alreadyInPool_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(true);

            assertThatThrownBy(() -> departmentService.addCourse(DEPT_ID, COURSE_ID))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(departmentCourseRepository, never()).save(any());
        }

        @Test
        @DisplayName("removeCourse — happy path deletes junction row")
        void removeCourse_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(true);

            departmentService.removeCourse(DEPT_ID, COURSE_ID);

            verify(departmentCourseRepository).deleteByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID);
        }

        @Test
        @DisplayName("removeCourse — not in pool throws ResourceNotFoundException")
        void removeCourse_notInPool_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            when(departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(DEPT_ID, COURSE_ID)).thenReturn(false);

            assertThatThrownBy(() -> departmentService.removeCourse(DEPT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(departmentCourseRepository, never()).deleteByIdDepartmentIdAndIdCourseId(any(), any());
        }

        @Test
        @DisplayName("removeCourse — department not found throws")
        void removeCourse_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.removeCourse(DEPT_ID, COURSE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findCoursesForDepartment — maps the department's pool")
        void findCoursesForDepartment_returnsAll() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentCourseRepository.findCoursesByDepartmentId(DEPT_ID)).thenReturn(List.of(course));

            List<CourseResponse> result = departmentService.findCoursesForDepartment(DEPT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).courseCode()).isEqualTo("MAT101");
        }

        @Test
        @DisplayName("findCoursesForDepartment — department not found throws")
        void findCoursesForDepartment_departmentNotFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.findCoursesForDepartment(DEPT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
