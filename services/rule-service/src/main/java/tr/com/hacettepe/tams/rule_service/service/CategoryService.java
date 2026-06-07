package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Category;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.dto.*;
import tr.com.hacettepe.tams.rule_service.exception.ConflictException;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.*;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for graduation category management.
 * Categories are scoped to a department and hold a pool of courses with
 * optional mandatory flags. Adds courses to a category only from that
 * department's registered course pool to maintain rule consistency.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryCourseRepository categoryCourseRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final DepartmentCourseRepository departmentCourseRepository;

    @Transactional
    public CategoryResponse create(UUID departmentId, CreateCategoryRequest request) {
        Department department = getDepartmentOrThrow(departmentId);
        if (categoryRepository.existsByDepartmentIdAndName(departmentId, request.name())) {
            throw new DuplicateResourceException(
                    "Category '" + request.name() + "' already exists in this department");
        }
        Category saved = categoryRepository.save(new Category(
                department, request.name(), request.description(),
                request.minCredit(), request.minEcts(), request.minCourseCount()));
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findByDepartment(UUID departmentId) {
        getDepartmentOrThrow(departmentId);
        return categoryRepository.findByDepartmentId(departmentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID departmentId, UUID categoryId) {
        getDepartmentOrThrow(departmentId);
        return CategoryResponse.from(getCategoryOrThrow(departmentId, categoryId));
    }

    @Transactional
    public CategoryResponse update(UUID departmentId, UUID categoryId, UpdateCategoryRequest request) {
        getDepartmentOrThrow(departmentId);
        Category category = getCategoryOrThrow(departmentId, categoryId);
        if (!category.getName().equals(request.name())
                && categoryRepository.existsByDepartmentIdAndName(departmentId, request.name())) {
            throw new DuplicateResourceException(
                    "Category name already taken in this department: " + request.name());
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setMinCredit(request.minCredit());
        category.setMinEcts(request.minEcts());
        category.setMinCourseCount(request.minCourseCount());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID departmentId, UUID categoryId) {
        getDepartmentOrThrow(departmentId);
        Category category = getCategoryOrThrow(departmentId, categoryId);
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryCourseResponse addCourse(UUID categoryId, CategoryCourseRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.courseId()));

        UUID departmentId = category.getDepartment().getId();
        if (!departmentCourseRepository.existsByIdDepartmentIdAndIdCourseId(departmentId, course.getId())) {
            throw new ConflictException(
                    "Course " + course.getCourseCode() + " is not in the department's course pool");
        }
        if (categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(categoryId, course.getId())) {
            throw new DuplicateResourceException(
                    "Course " + course.getCourseCode() + " is already in this category");
        }

        CategoryCourse saved = categoryCourseRepository.save(
                new CategoryCourse(category, course, request.isMandatory()));
        return CategoryCourseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryCourseResponse> findCourses(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        return categoryCourseRepository.findByCategoryIdWithCourse(categoryId).stream()
                .map(CategoryCourseResponse::from)
                .toList();
    }

    @Transactional
    public void removeCourse(UUID categoryId, UUID courseId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        if (!categoryCourseRepository.existsByIdCategoryIdAndIdCourseId(categoryId, courseId)) {
            throw new ResourceNotFoundException("Course not assigned to this category: " + courseId);
        }
        categoryCourseRepository.deleteByIdCategoryIdAndIdCourseId(categoryId, courseId);
    }

    @Transactional(readOnly = true)
    public RuleSetResponse getRuleSet(UUID departmentId) {
        Department department = getDepartmentOrThrow(departmentId);
        List<Category> categories = categoryRepository.findByDepartmentIdWithCourses(departmentId);
        List<RuleCategoryDto> categoryDtos = categories.stream()
                .map(RuleCategoryDto::from)
                .toList();
        return new RuleSetResponse(department.getId(), department.getName(), categoryDtos);
    }

    private Department getDepartmentOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Category getCategoryOrThrow(UUID departmentId, UUID categoryId) {
        return categoryRepository.findByIdAndDepartmentId(categoryId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found in department: " + categoryId));
    }
}
