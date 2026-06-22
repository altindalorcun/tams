package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.rule_service.domain.ExemptionRule;

import java.util.List;
import java.util.UUID;

/** Data access for {@link tr.com.hacettepe.tams.rule_service.domain.ExemptionRule} entities. */
public interface ExemptionRuleRepository extends JpaRepository<ExemptionRule, UUID> {

    List<ExemptionRule> findByDepartmentId(UUID departmentId);
}
