package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tr.com.hacettepe.tams.rule_service.domain.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for the global {@link Course} catalog. */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("SELECT c FROM Course c ORDER BY LOWER(c.courseCode) ASC")
    List<Course> findAllSortedByCourseCodeAsc();

    boolean existsByCourseCode(String courseCode);

    Optional<Course> findByCourseCode(String courseCode);
}
