package tr.com.hacettepe.tams.analysis_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.analysis_service.domain.CategoryResult;

import java.util.List;
import java.util.UUID;

/** Data access for {@link CategoryResult} entities. */
public interface CategoryResultRepository extends JpaRepository<CategoryResult, UUID> {

    List<CategoryResult> findByResultId(UUID resultId);
}
