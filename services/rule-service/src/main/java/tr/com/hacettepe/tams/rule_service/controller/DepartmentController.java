package tr.com.hacettepe.tams.rule_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tr.com.hacettepe.tams.rule_service.dto.*;
import tr.com.hacettepe.tams.rule_service.service.DepartmentService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for department management.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department CRUD and course pool management")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new department")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all departments")
    public ResponseEntity<List<DepartmentResponse>> findAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a department by ID")
    public ResponseEntity<DepartmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a department")
    public ResponseEntity<DepartmentResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a department and all its categories")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/courses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a course to the department's offering pool")
    public ResponseEntity<Void> addCourse(@PathVariable UUID id, @Valid @RequestBody AddCourseToPoolRequest request) {
        departmentService.addCourse(id, request.courseId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all courses offered by a department")
    public ResponseEntity<List<CourseResponse>> findCourses(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.findCoursesForDepartment(id));
    }

    @GetMapping("/{id}/course-pool")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get assigned and available courses for the department pool dialog")
    public ResponseEntity<DepartmentCoursePoolResponse> getCoursePool(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.findCoursePool(id));
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a course from the department's offering pool")
    public ResponseEntity<Void> removeCourse(@PathVariable UUID id, @PathVariable UUID courseId) {
        departmentService.removeCourse(id, courseId);
        return ResponseEntity.noContent().build();
    }
}
