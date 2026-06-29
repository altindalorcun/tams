package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tr.com.hacettepe.tams.rule_service.domain.Department;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link Department} entities. */
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    @Query("SELECT d FROM Department d ORDER BY LOWER(d.name) ASC")
    List<Department> findAllSortedByNameAsc();

    boolean existsByName(String name);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
