package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for the {@code department_courses} junction table.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DepartmentCourseId implements Serializable {

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "course_id")
    private UUID courseId;

    public DepartmentCourseId(UUID departmentId, UUID courseId) {
        this.departmentId = departmentId;
        this.courseId = courseId;
    }
}
