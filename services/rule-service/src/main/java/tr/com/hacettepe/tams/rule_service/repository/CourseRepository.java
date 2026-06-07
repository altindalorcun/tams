package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.hacettepe.tams.rule_service.domain.Course;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for the global {@link Course} catalog. */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCourseCode(String courseCode);

    Optional<Course> findByCourseCode(String courseCode);
}
