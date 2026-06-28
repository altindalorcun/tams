package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A curriculum-change equivalence rule that allows the graduation engine to treat
 * one set of courses as interchangeable with another set, based on the type of
 * curriculum change that occurred.
 *
 * <p>Rule types:
 * <ul>
 *   <li>{@code PAIRWISE} — Each {@code legacyCourseCode[i]} is individually equivalent to
 *       {@code replacementCourseCode[i]}. Bi-directional. Ignores effective date.
 *       Example: HAS222↔MUH103, HAS223↔MUH104.</li>
 *   <li>{@code GROUP_LEGACY_TO_REPLACEMENT} — All legacy codes passed (before effective date)
 *       imply all replacement codes. Example: FIZ103+FIZ104→FIZ117.</li>
 *   <li>{@code GROUP_REPLACEMENT_TO_LEGACY} — All replacement codes passed imply all legacy
 *       codes. Example: BBM479+BBM480→BBM419.</li>
 *   <li>{@code GROUP_MUTUAL} — Both GROUP directions apply. Example: BBM419↔BBM479+BBM480.</li>
 * </ul>
 */
@Entity
@Table(name = "curriculum_equivalence_rules")
@Getter
@Setter
@NoArgsConstructor
public class CurriculumEquivalenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * Discriminates how the engine applies this rule.
     * One of: PAIRWISE, GROUP_LEGACY_TO_REPLACEMENT, GROUP_REPLACEMENT_TO_LEGACY, GROUP_MUTUAL.
     */
    @Column(name = "rule_type", nullable = false, length = 30)
    private String ruleType;

    /** Courses removed from the curriculum (the "old" set). */
    @Column(name = "legacy_course_codes", nullable = false, columnDefinition = "TEXT[]")
    private String[] legacyCourseCodes = new String[0];

    /** Courses added to the curriculum (the "new" set). */
    @Column(name = "replacement_course_codes", nullable = false, columnDefinition = "TEXT[]")
    private String[] replacementCourseCodes = new String[0];

    /**
     * Academic year in which the curriculum change took effect, e.g. {@code 2019} for 2019-2020.
     * Used by GROUP rules to validate that legacy courses were taken before the change.
     * Ignored by PAIRWISE rules.
     */
    @Column(name = "effective_from_year")
    private Integer effectiveFromYear;

    /**
     * Term within the effective year: {@code GUZ} (fall) or {@code BAHAR} (spring).
     * Null means the effective boundary is at the start of the academic year.
     */
    @Column(name = "effective_from_term", length = 10)
    private String effectiveFromTerm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    public CurriculumEquivalenceRule(Department department, String ruleType,
                                     String[] legacyCourseCodes, String[] replacementCourseCodes,
                                     Integer effectiveFromYear, String effectiveFromTerm) {
        this.department = department;
        this.ruleType = ruleType;
        this.legacyCourseCodes = legacyCourseCodes;
        this.replacementCourseCodes = replacementCourseCodes;
        this.effectiveFromYear = effectiveFromYear;
        this.effectiveFromTerm = effectiveFromTerm;
    }
}
