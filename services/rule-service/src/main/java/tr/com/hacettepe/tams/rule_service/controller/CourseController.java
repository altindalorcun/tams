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
import tr.com.hacettepe.tams.rule_service.dto.CourseResponse;
import tr.com.hacettepe.tams.rule_service.dto.CreateCourseRequest;
import tr.com.hacettepe.tams.rule_service.dto.UpdateCourseRequest;
import tr.com.hacettepe.tams.rule_service.service.CourseService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the global course catalog.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Institution-wide course catalog management")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a new course to the global catalog")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = courseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all courses in the global catalog")
    public ResponseEntity<List<CourseResponse>> findAll() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a course by ID")
    public ResponseEntity<CourseResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a course")
    public ResponseEntity<CourseResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a course from the global catalog")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
