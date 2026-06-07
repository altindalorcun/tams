package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction entity for the many-to-many relationship between
 * {@link Department} and {@link Course}.
 * Represents which courses are offered by a given department.
 */
@Entity
@Table(name = "department_courses")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentCourse {

    @EmbeddedId
    private DepartmentCourseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("departmentId")
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    public DepartmentCourse(Department department, Course course) {
        this.id = new DepartmentCourseId(department.getId(), course.getId());
        this.department = department;
        this.course = course;
    }
}
