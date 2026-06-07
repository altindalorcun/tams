package tr.com.hacettepe.tams.auth_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for the {@link TeacherStudentMap} entity.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TeacherStudentId implements Serializable {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;
}
