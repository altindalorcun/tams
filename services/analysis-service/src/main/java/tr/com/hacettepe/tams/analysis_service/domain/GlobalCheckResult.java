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
 * Persisted outcome of a department-level global graduation rule
 * (total ECTS threshold or fail-grade block).
 */
@Entity
@Table(name = "global_check_results")
@Getter
@Setter
@NoArgsConstructor
public class GlobalCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private AnalysisResult result;

    @Column(name = "check_type", nullable = false, length = 20)
    private String checkType;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(name = "required_min_ects", precision = 6, scale = 2)
    private BigDecimal requiredMinEcts;

    @Column(name = "earned_ects", precision = 6, scale = 2)
    private BigDecimal earnedEcts;

    @Column(name = "failed_course_codes", columnDefinition = "TEXT[]")
    private String[] failedCourseCodes = new String[0];

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
