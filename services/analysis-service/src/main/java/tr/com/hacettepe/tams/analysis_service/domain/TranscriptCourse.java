package tr.com.hacettepe.tams.analysis_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot of a single course row taken from the parsed transcript.
 * Contains no PII — student identity lives only in {@link AnalysisResult#studentNumber}.
 */
@Entity
@Table(name = "transcript_courses")
@Getter
@Setter
@NoArgsConstructor
public class TranscriptCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private AnalysisResult result;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "credit", nullable = false, precision = 4, scale = 2)
    private BigDecimal credit;

    @Column(name = "ects", nullable = false, precision = 4, scale = 2)
    private BigDecimal ects;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "is_passed", nullable = false)
    private boolean passed;
}
