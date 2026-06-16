package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.dto.CourseResponse;
import tr.com.hacettepe.tams.rule_service.dto.CreateCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCourseRequest;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CourseRepository;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for the global course catalog.
 * Courses are institution-wide; they are shared across departments and categories.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        if (courseRepository.existsByCourseCode(request.courseCode())) {
            throw new DuplicateResourceException("Course code already exists: " + request.courseCode());
        }
        Course saved = courseRepository.save(
                new Course(request.courseCode(), request.name(), request.credits(), request.ects()));
        return CourseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(UUID id) {
        return CourseResponse.from(getCourseOrThrow(id));
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
        return CourseResponse.from(courseRepository.save(course));
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
}
