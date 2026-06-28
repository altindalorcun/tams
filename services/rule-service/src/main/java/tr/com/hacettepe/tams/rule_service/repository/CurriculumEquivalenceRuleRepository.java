package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.rule_service.domain.CurriculumEquivalenceRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Data access for {@link CurriculumEquivalenceRule} entities. */
public interface CurriculumEquivalenceRuleRepository extends JpaRepository<CurriculumEquivalenceRule, UUID> {

    List<CurriculumEquivalenceRule> findByDepartmentId(UUID departmentId);

    Optional<CurriculumEquivalenceRule> findByIdAndDepartmentId(UUID id, UUID departmentId);
}
