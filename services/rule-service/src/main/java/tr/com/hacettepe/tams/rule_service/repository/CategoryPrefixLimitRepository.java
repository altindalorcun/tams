package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.rule_service.domain.CategoryPrefixLimit;

import java.util.List;
import java.util.UUID;

/** Repository for {@link tr.com.hacettepe.tams.rule_service.domain.CategoryPrefixLimit} entities. */
public interface CategoryPrefixLimitRepository extends JpaRepository<CategoryPrefixLimit, UUID> {

    List<CategoryPrefixLimit> findByCategoryId(UUID categoryId);
}
