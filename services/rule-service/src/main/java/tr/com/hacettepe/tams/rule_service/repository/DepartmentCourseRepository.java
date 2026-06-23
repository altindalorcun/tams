package tr.com.hacettepe.tams.rule_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.hacettepe.tams.rule_service.domain.Course;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourse;
import tr.com.hacettepe.tams.rule_service.domain.DepartmentCourseId;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for the {@code department_courses} junction table. */
public interface DepartmentCourseRepository extends JpaRepository<DepartmentCourse, DepartmentCourseId> {

    boolean existsByIdDepartmentIdAndIdCourseId(UUID departmentId, UUID courseId);

    void deleteByIdDepartmentIdAndIdCourseId(UUID departmentId, UUID courseId);

    @Query("SELECT dc.course FROM DepartmentCourse dc WHERE dc.id.departmentId = :departmentId")
    List<Course> findCoursesByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT dc.id.departmentId FROM DepartmentCourse dc WHERE dc.id.courseId = :courseId")
    List<UUID> findDepartmentIdsByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT dc.id FROM DepartmentCourse dc")
    List<DepartmentCourseId> findAllIds();
}
