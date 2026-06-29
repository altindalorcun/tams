package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Category} entities. */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByDepartmentId(UUID departmentId);

    @Query("SELECT c FROM Category c WHERE c.department.id = :departmentId ORDER BY LOWER(c.name) ASC")
    List<Category> findByDepartmentIdSortedByNameAsc(@Param("departmentId") UUID departmentId);

    Optional<Category> findByIdAndDepartmentId(UUID id, UUID departmentId);

    boolean existsByDepartmentIdAndName(UUID departmentId, String name);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.categoryCourses cc LEFT JOIN FETCH cc.course WHERE c.department.id = :departmentId")
    List<Category> findByDepartmentIdWithCourses(@Param("departmentId") UUID departmentId);
}
