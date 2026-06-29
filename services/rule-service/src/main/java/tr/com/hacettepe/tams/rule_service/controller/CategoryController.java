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
import tr.com.hacettepe.tams.rule_service.service.CategoryService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for graduation category management.
 * Categories are scoped to a department. All endpoints require ADMIN role.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Graduation category management and course pool assignment")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/api/v1/departments/{deptId}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a graduation category under a department")
    public ResponseEntity<CategoryResponse> create(@PathVariable UUID deptId,
                                                   @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.create(deptId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/departments/{deptId}/categories/{catId}")
                .buildAndExpand(deptId, response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/departments/{deptId}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all graduation categories for a department")
    public ResponseEntity<List<CategoryResponse>> findByDepartment(@PathVariable UUID deptId) {
        return ResponseEntity.ok(categoryService.findByDepartment(deptId));
    }

    @GetMapping("/api/v1/departments/{deptId}/categories/{catId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a graduation category by ID")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID deptId, @PathVariable UUID catId) {
        return ResponseEntity.ok(categoryService.findById(deptId, catId));
    }

    @PutMapping("/api/v1/departments/{deptId}/categories/{catId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a graduation category")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID deptId,
                                                   @PathVariable UUID catId,
                                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(deptId, catId, request));
    }

    @DeleteMapping("/api/v1/departments/{deptId}/categories/{catId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a graduation category")
    public ResponseEntity<Void> delete(@PathVariable UUID deptId, @PathVariable UUID catId) {
        categoryService.delete(deptId, catId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/categories/{catId}/courses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a course to a graduation category (must be in the department's pool)")
    public ResponseEntity<CategoryCourseResponse> addCourse(@PathVariable UUID catId,
                                                            @Valid @RequestBody CategoryCourseRequest request) {
        CategoryCourseResponse response = categoryService.addCourse(catId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/v1/categories/{catId}/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a course assignment within a graduation category")
    public ResponseEntity<CategoryCourseResponse> updateCourse(@PathVariable UUID catId,
                                                               @PathVariable UUID courseId,
                                                               @Valid @RequestBody UpdateCategoryCourseRequest request) {
        return ResponseEntity.ok(categoryService.updateCourse(catId, courseId, request));
    }

    @GetMapping("/api/v1/categories/{catId}/courses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all courses assigned to a graduation category")
    public ResponseEntity<List<CategoryCourseResponse>> findCourses(@PathVariable UUID catId) {
        return ResponseEntity.ok(categoryService.findCourses(catId));
    }

    @DeleteMapping("/api/v1/categories/{catId}/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a course from a graduation category")
    public ResponseEntity<Void> removeCourse(@PathVariable UUID catId, @PathVariable UUID courseId) {
        categoryService.removeCourse(catId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/categories/{catId}/prefix-limits")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a course-code prefix limit to a graduation category")
    public ResponseEntity<PrefixLimitDto> addPrefixLimit(@PathVariable UUID catId,
                                                         @Valid @RequestBody CreatePrefixLimitRequest request) {
        PrefixLimitDto created = categoryService.addPrefixLimit(catId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/categories/{catId}/prefix-limits/{limitId}")
                .buildAndExpand(catId, created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/api/v1/categories/{catId}/prefix-limits/{limitId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a course-code prefix limit from a graduation category")
    public ResponseEntity<Void> removePrefixLimit(@PathVariable UUID catId, @PathVariable UUID limitId) {
        categoryService.removePrefixLimit(catId, limitId);
        return ResponseEntity.noContent().build();
    }
}
