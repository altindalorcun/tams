package tr.com.hacettepe.tams.analysis_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One unfulfilled graduation requirement category within an analysis result.
 * missing_courses holds the course codes the student has not yet passed.
 */
@Entity
@Table(name = "deficiencies")
@Getter
@Setter
@NoArgsConstructor
public class Deficiency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private AnalysisResult result;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "required_credit", nullable = false, precision = 5, scale = 2)
    private BigDecimal requiredCredit;

    @Column(name = "earned_credit", nullable = false, precision = 5, scale = 2)
    private BigDecimal earnedCredit;

    @Column(name = "required_ects", nullable = false, precision = 5, scale = 2)
    private BigDecimal requiredEcts;

    @Column(name = "earned_ects", nullable = false, precision = 5, scale = 2)
    private BigDecimal earnedEcts;

    @Column(name = "missing_courses", columnDefinition = "TEXT[]")
    private String[] missingCourses;
}
