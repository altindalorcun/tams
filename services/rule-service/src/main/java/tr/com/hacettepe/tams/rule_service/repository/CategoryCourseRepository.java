package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourse;
import tr.com.hacettepe.tams.rule_service.domain.CategoryCourseId;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for the {@code category_courses} junction table. */
public interface CategoryCourseRepository extends JpaRepository<CategoryCourse, CategoryCourseId> {

    boolean existsByIdCategoryIdAndIdCourseId(UUID categoryId, UUID courseId);

    void deleteByIdCategoryIdAndIdCourseId(UUID categoryId, UUID courseId);

    @Query("SELECT cc FROM CategoryCourse cc JOIN FETCH cc.course WHERE cc.id.categoryId = :categoryId")
    List<CategoryCourse> findByCategoryIdWithCourse(@Param("categoryId") UUID categoryId);
}
