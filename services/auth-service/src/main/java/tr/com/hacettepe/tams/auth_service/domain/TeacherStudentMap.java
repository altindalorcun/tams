package tr.com.hacettepe.tams.auth_service.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Maps a student to the teacher who uploaded their transcript.
 * A student can only see their results once this relationship exists.
 */
@Entity
@Table(name = "teacher_student_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherStudentMap {

    @EmbeddedId
    private TeacherStudentId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
