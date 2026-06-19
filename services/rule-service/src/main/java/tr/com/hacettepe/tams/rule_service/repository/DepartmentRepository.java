package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.rule_service.domain.Department;

import java.util.UUID;

/** Spring Data JPA repository for {@link Department} entities. */
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByName(String name);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
