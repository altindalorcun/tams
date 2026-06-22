package tr.com.hacettepe.tams.analysis_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted evaluation outcome for a single graduation requirement category.
 * Both satisfied and unsatisfied categories are recorded, enabling per-category
 * progress display in the frontend. Rows with {@code satisfied = false} serve
 * the role previously held by {@link Deficiency}.
 */
@Entity
@Table(name = "category_results")
@Getter
@Setter
@NoArgsConstructor
public class CategoryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private AnalysisResult result;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "satisfied", nullable = false)
    private boolean satisfied;

    @Column(name = "required_credit", nullable = false, precision = 5, scale = 2)
    private BigDecimal requiredCredit = BigDecimal.ZERO;

    @Column(name = "earned_credit", nullable = false, precision = 5, scale = 2)
    private BigDecimal earnedCredit = BigDecimal.ZERO;

    @Column(name = "required_ects", nullable = false, precision = 5, scale = 2)
    private BigDecimal requiredEcts = BigDecimal.ZERO;

    @Column(name = "earned_ects", nullable = false, precision = 5, scale = 2)
    private BigDecimal earnedEcts = BigDecimal.ZERO;

    @Column(name = "required_course_count", nullable = false)
    private int requiredCourseCount;

    @Column(name = "earned_course_count", nullable = false)
    private int earnedCourseCount;

    @Column(name = "missing_mandatory_courses", columnDefinition = "TEXT[]")
    private String[] missingMandatoryCourses = new String[0];

    @Column(name = "cohort_skipped", nullable = false)
    private boolean cohortSkipped = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
