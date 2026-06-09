package tr.com.hacettepe.tams.analysis_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.analysis_service.domain.TranscriptCourse;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link TranscriptCourse} entities.
 */
public interface TranscriptCourseRepository extends JpaRepository<TranscriptCourse, UUID> {

    List<TranscriptCourse> findByResultId(UUID resultId);
}
