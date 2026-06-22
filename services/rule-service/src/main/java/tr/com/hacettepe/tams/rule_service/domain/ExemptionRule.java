package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A substitution rule that grants a student credit for a course they have not taken,
 * provided they have passed all courses listed in {@code requiredCourseCodes}.
 *
 * <p>Example: passing FIZ103 + FIZ104 makes the engine treat FIZ117 as passed.
 */
@Entity
@Table(name = "exemption_rules")
@Getter
@Setter
@NoArgsConstructor
public class ExemptionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "required_course_codes", nullable = false, columnDefinition = "TEXT[]")
    private String[] requiredCourseCodes = new String[0];

    @Column(name = "exempted_course_code", nullable = false, length = 20)
    private String exemptedCourseCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    public ExemptionRule(Department department, String[] requiredCourseCodes, String exemptedCourseCode) {
        this.department = department;
        this.requiredCourseCodes = requiredCourseCodes;
        this.exemptedCourseCode = exemptedCourseCode;
    }
}
