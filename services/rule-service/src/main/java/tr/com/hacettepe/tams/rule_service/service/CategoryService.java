package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Category;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;
import tr.com.hacettepe.tams.rule_service.domain.CategoryPrefixLimit;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.dto.*;
import tr.com.hacettepe.tams.rule_service.exception.ConflictException;
import tr.com.hacettepe.tams.rule_service.exception.DuplicateResourceException;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.rule_service.repository.*;
import tr.com.hacettepe.tams.rule_service.util.EnrollmentCohortBoundaryValidator;

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
    private final CategoryPrefixLimitRepository categoryPrefixLimitRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final DepartmentCourseRepository departmentCourseRepository;
    private final CurriculumEquivalenceRuleRepository curriculumEquivalenceRuleRepository;

    @Transactional
    public CategoryResponse create(UUID departmentId, CreateCategoryRequest request) {
        Department department = getDepartmentOrThrow(departmentId);
        if (categoryRepository.existsByDepartmentIdAndName(departmentId, request.name())) {
            throw new DuplicateResourceException(
                    "Category '" + request.name() + "' already exists in this department");
        }
        Category category = new Category(
                department, request.name(), request.description(),
                request.minCredit(), request.minEcts(), request.minCourseCount());
        category.setAppliesFromYear(request.appliesFromYear());
        category.setAppliesToYear(request.appliesToYear());
        category.setConditionCourseCodes(toArray(request.conditionCourseCodes()));
        category.setMinCourseCountIfMet(request.minCourseCountIfMet());
        category.setMinEctsIfMet(request.minEctsIfMet());
        Category saved = categoryRepository.save(category);
        syncPrefixLimits(saved, request.prefixLimits());
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
        category.setAppliesFromYear(request.appliesFromYear());
        category.setAppliesToYear(request.appliesToYear());
        category.setConditionCourseCodes(toArray(request.conditionCourseCodes()));
        category.setMinCourseCountIfMet(request.minCourseCountIfMet());
        category.setMinEctsIfMet(request.minEctsIfMet());
        syncPrefixLimits(category, request.prefixLimits());
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

        EnrollmentCohortBoundaryValidator.validate(
                request.appliesFromYear(), request.appliesFromTerm(),
                request.appliesToYear(), request.appliesToTerm());

        CategoryCourse cc = new CategoryCourse(category, course, request.isMandatory());
        applyAppliesBounds(cc, request.appliesFromYear(), request.appliesFromTerm(),
                request.appliesToYear(), request.appliesToTerm());
        CategoryCourse saved = categoryCourseRepository.save(cc);
        return CategoryCourseResponse.from(saved);
    }

    @Transactional
    public CategoryCourseResponse updateCourse(UUID categoryId, UUID courseId,
                                             UpdateCategoryCourseRequest request) {
        CategoryCourse cc = getCategoryCourseOrThrow(categoryId, courseId);
        EnrollmentCohortBoundaryValidator.validate(
                request.appliesFromYear(), request.appliesFromTerm(),
                request.appliesToYear(), request.appliesToTerm());
        cc.setMandatory(request.isMandatory());
        applyAppliesBounds(cc, request.appliesFromYear(), request.appliesFromTerm(),
                request.appliesToYear(), request.appliesToTerm());
        return CategoryCourseResponse.from(categoryCourseRepository.save(cc));
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
        List<CurriculumEquivalenceRuleDto> equivalenceDtos =
                curriculumEquivalenceRuleRepository.findByDepartmentId(departmentId).stream()
                        .map(CurriculumEquivalenceRuleDto::from)
                        .toList();
        return new RuleSetResponse(
                department.getId(),
                department.getName(),
                department.getMinTotalEcts(),
                department.isBlockOnAnyFGrade(),
                categoryDtos,
                equivalenceDtos);
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

    @Transactional
    public PrefixLimitDto addPrefixLimit(UUID categoryId, CreatePrefixLimitRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        CategoryPrefixLimit limit = new CategoryPrefixLimit(
                category,
                request.courseCodePrefix().toUpperCase(),
                request.maxCount()
        );
        return PrefixLimitDto.from(categoryPrefixLimitRepository.save(limit));
    }

    @Transactional
    public void removePrefixLimit(UUID categoryId, UUID limitId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        CategoryPrefixLimit limit = categoryPrefixLimitRepository.findById(limitId)
                .orElseThrow(() -> new ResourceNotFoundException("Prefix limit not found: " + limitId));
        if (!limit.getCategory().getId().equals(categoryId)) {
            throw new ResourceNotFoundException("Prefix limit does not belong to category: " + categoryId);
        }
        categoryPrefixLimitRepository.delete(limit);
    }

    private CategoryCourse getCategoryCourseOrThrow(UUID categoryId, UUID courseId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        return categoryCourseRepository.findByCategoryIdWithCourse(categoryId).stream()
                .filter(cc -> cc.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not assigned to this category: " + courseId));
    }

    private void applyAppliesBounds(CategoryCourse cc,
                                    Integer appliesFromYear, String appliesFromTerm,
                                    Integer appliesToYear, String appliesToTerm) {
        cc.setAppliesFromYear(appliesFromYear);
        cc.setAppliesFromTerm(normalizeTerm(appliesFromTerm));
        cc.setAppliesToYear(appliesToYear);
        cc.setAppliesToTerm(normalizeTerm(appliesToTerm));
    }

    /** Returns null for blank input; otherwise upper-case GUZ or BAHAR. */
    private String normalizeTerm(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        return term.toUpperCase();
    }

    /** Converts a nullable list of course codes to a String array, normalising each code to upper-case. */
    private String[] toArray(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new String[0];
        }
        return codes.stream().map(String::toUpperCase).toArray(String[]::new);
    }

    /**
     * Replaces all prefix limits on {@code category} with the entries from {@code limits}.
     * Null or empty input clears existing limits.
     */
    private void syncPrefixLimits(Category category, List<CreatePrefixLimitRequest> limits) {
        category.getPrefixLimits().clear();
        if (limits == null || limits.isEmpty()) {
            return;
        }
        for (CreatePrefixLimitRequest limit : limits) {
            category.getPrefixLimits().add(new CategoryPrefixLimit(
                    category,
                    limit.courseCodePrefix().toUpperCase(),
                    limit.maxCount()
            ));
        }
    }
}
