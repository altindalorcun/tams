package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourse;
import tr.com.hacettepe.tams.rule_service.dto.*;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for department management.
 * Handles CRUD operations and the department course pool (department_courses junction).
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final DepartmentCourseRepository departmentCourseRepository;

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department already exists: " + request.name());
        }
        if (departmentRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Department code already taken: " + request.code());
        }
        Department saved = departmentRepository.save(new Department(
                request.name(), request.code(), request.description(),
                request.minTotalEcts(),
                request.blockOnAnyFGrade() != null && request.blockOnAnyFGrade()));
        return DepartmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAllSortedByNameAsc().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(UUID id) {
        return DepartmentResponse.from(getDepartmentOrThrow(id));
    }

    @Transactional
    public DepartmentResponse update(UUID id, UpdateDepartmentRequest request) {
        Department department = getDepartmentOrThrow(id);
        if (!department.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department name already taken: " + request.name());
        }
        if (!department.getCode().equals(request.code()) && departmentRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Department code already taken: " + request.code());
        }
        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description());
        department.setMinTotalEcts(request.minTotalEcts());
        department.setBlockOnAnyFGrade(request.blockOnAnyFGrade() != null && request.blockOnAnyFGrade());
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found: " + id);
        }
        departmentRepository.deleteById(id);
    }

    @Transactional
    public void addCourse(UUID departmentId, UUID courseId) {
        Department department = getDepartmentOrThrow(departmentId);
        Course course = getCourseOrThrow(courseId);
        if (departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(departmentId, courseId)) {
            throw new DuplicateResourceException("Course already in department pool: " + course.getCourseCode());
        }
        departmentCourseRepository.save(new DepartmentCourse(department, course));
    }

    @Transactional
    public void removeCourse(UUID departmentId, UUID courseId) {
        getDepartmentOrThrow(departmentId);
        getCourseOrThrow(courseId);
        if (!departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(departmentId, courseId)) {
            throw new ResourceNotFoundException("Course not found in department pool: " + courseId);
        }
        departmentCourseRepository.deleteByIdDepartmentIdAndIdCourseId(departmentId, courseId);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findCoursesForDepartment(UUID departmentId) {
        getDepartmentOrThrow(departmentId);
        return departmentCourseRepository.findCoursesByDepartmentId(departmentId).stream()
                .map(CourseResponse::from)
                .toList();
    }

    /**
     * Returns assigned and available courses for a department in one query,
     * so the frontend can populate the course-pool dialog with a single request.
     */
    @Transactional(readOnly = true)
    public DepartmentCoursePoolResponse findCoursePool(UUID departmentId) {
        getDepartmentOrThrow(departmentId);
        List<Course> assigned = departmentCourseRepository.findCoursesByDepartmentId(departmentId);
        Set<UUID> assignedIds = assigned.stream().map(Course::getId).collect(Collectors.toSet());
        List<Course> available = courseRepository.findAll().stream()
                .filter(c -> !assignedIds.contains(c.getId()))
                .toList();
        return new DepartmentCoursePoolResponse(
                assigned.stream().map(DepartmentCourseItem::from).toList(),
                available.stream().map(CourseResponse::from).toList()
        );
    }

    Department getDepartmentOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Course getCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }
}
