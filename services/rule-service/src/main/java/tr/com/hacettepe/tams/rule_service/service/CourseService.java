package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourseId;
import tr.com.hacettepe.tams.rule_service.dto.CourseResponse;
import tr.com.hacettepe.tams.rule_service.dto.CreateCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCourseRequest;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentCourseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for the global course catalog.
 * Courses are institution-wide; they are shared across departments and categories.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentCourseRepository departmentCourseRepository;

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        if (courseRepository.existsByCourseCode(request.courseCode())) {
            throw new DuplicateResourceException("Course code already exists: " + request.courseCode());
        }
        Course saved = courseRepository.save(
                new Course(request.courseCode(), request.courseName(), request.credit(), request.ects()));
        return CourseResponse.from(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        Map<UUID, List<UUID>> departmentIdsByCourseId = buildDepartmentIdsByCourseId();
        return courseRepository.findAllSortedByCourseCodeAsc().stream()
                .map(course -> CourseResponse.from(course,
                        departmentIdsByCourseId.getOrDefault(course.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(UUID id) {
        Course course = getCourseOrThrow(id);
        return CourseResponse.from(course, departmentCourseRepository.findDepartmentIdsByCourseId(id));
    }

    @Transactional
    public CourseResponse update(UUID id, UpdateCourseRequest request) {
        Course course = getCourseOrThrow(id);
        if (!course.getCourseCode().equals(request.courseCode())
                && courseRepository.existsByCourseCode(request.courseCode())) {
            throw new DuplicateResourceException("Course code already taken: " + request.courseCode());
        }
        course.setCourseCode(request.courseCode());
        course.setCourseName(request.courseName());
        course.setCredit(request.credit());
        course.setEcts(request.ects());
        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved, departmentCourseRepository.findDepartmentIdsByCourseId(id));
    }

    @Transactional
    public void delete(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        courseRepository.deleteById(id);
    }

    Course getCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private Map<UUID, List<UUID>> buildDepartmentIdsByCourseId() {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (DepartmentCourseId id : departmentCourseRepository.findAllIds()) {
            map.computeIfAbsent(id.getCourseId(), ignored -> new ArrayList<>()).add(id.getDepartmentId());
        }
        return map;
    }
}
