package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full result with per-category evaluation breakdown and course snapshot.
 * Returned by {@code GET /api/v1/results/{id}}, {@code GET /api/v1/results/me},
 * and {@code GET /api/v1/results/by-job/{jobId}}.
 */
@Schema(description = "Full graduation analysis result including per-category evaluations and transcript course snapshot")
public record AnalysisResultDetailResponse(
        @Schema(description = "Primary key of this result record")
        UUID id,
        @Schema(description = "Kafka job ID linking the upload to this result")
        String jobId,
        @Schema(description = "Öğrenci No extracted from the transcript")
        String studentNumber,
        @Schema(description = "UUID of the department whose graduation rules were applied")
        UUID departmentId,
        @Schema(description = "Name of the department whose graduation rules were applied")
        String departmentName,
        @Schema(description = "Analysis lifecycle status", allowableValues = {"PENDING", "COMPLETED", "FAILED"})
        String status,
        @Schema(description = "True when the student satisfies all graduation requirements; null while PENDING")
        Boolean isEligible,
        @Schema(description = "Cumulative GPA computed using the Hacettepe 4.00 grading scale")
        BigDecimal gpa,
        @Schema(description = "Total completed credit hours across all categories")
        BigDecimal totalCredit,
        @Schema(description = "Total completed ECTS points across all categories")
        BigDecimal totalEcts,
        @Schema(description = "Timestamp when the upload was received")
        OffsetDateTime createdAt,
        @Schema(description = "Timestamp when analysis completed; null while PENDING")
        OffsetDateTime completedAt,
        @Schema(description = "Per-category evaluation results (both satisfied and unsatisfied)")
        List<CategoryResultResponse> categoryResults,
        @Schema(description = "Department-level global rule evaluations (total ECTS, fail-grade block)")
        List<GlobalCheckResultResponse> globalCheckResults,
        @Schema(description = "All courses from the parsed transcript snapshot")
        List<TranscriptCourseResponse> courses
) {
    /**
     * Factory method that maps an {@link AnalysisResult} entity to this response record.
     * Must be called within an active transaction so lazy collections can be accessed.
     */
    public static AnalysisResultDetailResponse from(AnalysisResult r) {
        List<CategoryResultResponse> categoryResults = r.getCategoryResults().stream()
                .map(CategoryResultResponse::from)
                .toList();
        List<TranscriptCourseResponse> courses = r.getTranscriptCourses().stream()
                .map(TranscriptCourseResponse::from)
                .toList();
        List<GlobalCheckResultResponse> globalCheckResults = r.getGlobalCheckResults().stream()
                .map(GlobalCheckResultResponse::from)
                .toList();
        return new AnalysisResultDetailResponse(
                r.getId(),
                r.getJobId(),
                r.getStudentNumber(),
                r.getDepartmentId(),
                r.getDepartmentName(),
                r.getStatus().name(),
                r.getIsEligible(),
                r.getGpa(),
                r.getTotalCredit(),
                r.getTotalEcts(),
                r.getCreatedAt(),
                r.getCompletedAt(),
                categoryResults,
                globalCheckResults,
                courses
        );
    }
}
