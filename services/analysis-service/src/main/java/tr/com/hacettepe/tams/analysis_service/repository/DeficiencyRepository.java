package tr.com.hacettepe.tams.analysis_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.analysis_service.domain.Deficiency;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link Deficiency} entities.
 */
public interface DeficiencyRepository extends JpaRepository<Deficiency, UUID> {

    List<Deficiency> findByResultId(UUID resultId);
}
