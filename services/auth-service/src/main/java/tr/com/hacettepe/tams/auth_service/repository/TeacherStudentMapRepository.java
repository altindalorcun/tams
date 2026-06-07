package tr.com.hacettepe.tams.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tr.com.hacettepe.tams.auth_service.domain.TeacherStudentId;
import tr.com.hacettepe.tams.auth_service.domain.TeacherStudentMap;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link TeacherStudentMap} entities.
 */
@Repository
public interface TeacherStudentMapRepository extends JpaRepository<TeacherStudentMap, TeacherStudentId> {

    boolean existsByIdStudentId(UUID studentId);

    Optional<TeacherStudentMap> findByIdStudentId(UUID studentId);
}
