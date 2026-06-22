package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A graduation requirement group scoped to a specific department.
 * Examples: "Bölüm Zorunlu", "Teknik Seçmeli", "Bölüm Dışı Seçmeli".
 *
 * <p>Three independent thresholds are evaluated by the graduation engine:
 * <ul>
 *   <li>{@code minCourseCount} — student must pass at least this many courses from the pool</li>
 *   <li>{@code minCredit} — cumulative credits of passed courses must reach this threshold</li>
 *   <li>{@code minEcts} — cumulative ECTS of passed courses must reach this threshold</li>
 * </ul>
 * Additionally, courses flagged {@code is_mandatory} in {@link CategoryCourse} must each be passed
 * individually regardless of the above thresholds.
 *
 * <p>Conditional thresholds: if the student has passed at least one course whose code appears in
 * {@code conditionCourseCodes}, the engine uses {@code minCourseCountIfMet} / {@code minEctsIfMet}
 * instead of the base thresholds (when those fields are non-null).
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_credit", nullable = false, precision = 5, scale = 2)
    private BigDecimal minCredit = BigDecimal.ZERO;

    @Column(name = "min_ects", nullable = false, precision = 5, scale = 2)
    private BigDecimal minEcts = BigDecimal.ZERO;

    @Column(name = "min_course_count", nullable = false)
    private int minCourseCount = 0;

    @Column(name = "applies_from_year")
    private Integer appliesFromYear;

    @Column(name = "applies_to_year")
    private Integer appliesToYear;

    @Column(name = "condition_course_codes", columnDefinition = "TEXT[]")
    private String[] conditionCourseCodes = new String[0];

    @Column(name = "min_course_count_if_met")
    private Integer minCourseCountIfMet;

    @Column(name = "min_ects_if_met", precision = 5, scale = 2)
    private BigDecimal minEctsIfMet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryCourse> categoryCourses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryPrefixLimit> prefixLimits = new ArrayList<>();

    public Category(Department department, String name, String description,
                    BigDecimal minCredit, BigDecimal minEcts, int minCourseCount) {
        this.department = department;
        this.name = name;
        this.description = description;
        this.minCredit = minCredit;
        this.minEcts = minEcts;
        this.minCourseCount = minCourseCount;
    }
}
