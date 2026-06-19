package tr.com.hacettepe.tams.analysis_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persisted record of a single transcript analysis run.
 * A result is created with status PENDING at upload time and updated to
 * COMPLETED or FAILED once the graduation engine finishes.
 */
@Entity
@Table(name = "analysis_results")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "masked_student_ref", length = 80)
    private String maskedStudentRef;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(name = "is_eligible")
    private Boolean isEligible;

    @Column(name = "total_credit", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "total_ects", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalEcts = BigDecimal.ZERO;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "gpa", precision = 4, scale = 2)
    private BigDecimal gpa;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryResult> categoryResults = new ArrayList<>();

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TranscriptCourse> transcriptCourses = new ArrayList<>();
}
