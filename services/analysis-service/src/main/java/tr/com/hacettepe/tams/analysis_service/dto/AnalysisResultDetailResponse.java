package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full result with deficiency breakdown and course snapshot.
 * Returned by {@code GET /api/v1/results/{id}} and {@code GET /api/v1/results/me}.
 */
@Schema(description = "Full graduation analysis result including per-category deficiencies and transcript course snapshot")
public record AnalysisResultDetailResponse(
        @Schema(description = "Primary key of this result record")
        UUID id,
        @Schema(description = "Kafka job ID linking the upload to this result", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String jobId,
        @Schema(description = "SHA-256-derived masked student identifier — no raw PII", example = "a3f1c2d4e5b6a7c8")
        String maskedStudentRef,
        @Schema(description = "UUID of the department whose graduation rules were applied")
        UUID departmentId,
        @Schema(description = "Analysis lifecycle status", allowableValues = {"PENDING", "COMPLETED", "FAILED"}, example = "COMPLETED")
        String status,
        @Schema(description = "True when the student satisfies all graduation requirements; null while PENDING")
        Boolean isEligible,
        @Schema(description = "Total completed credit hours across all categories", example = "128.0")
        BigDecimal totalCredit,
        @Schema(description = "Total completed ECTS points across all categories", example = "240.0")
        BigDecimal totalEcts,
        @Schema(description = "Timestamp when the upload was received")
        OffsetDateTime createdAt,
        @Schema(description = "Timestamp when analysis completed; null while PENDING")
        OffsetDateTime completedAt,
        @Schema(description = "List of unsatisfied graduation requirement categories; empty when fully eligible")
        List<DeficiencyResponse> deficiencies,
        @Schema(description = "All courses from the parsed transcript snapshot")
        List<TranscriptCourseResponse> courses
) {
    public static AnalysisResultDetailResponse from(AnalysisResult r) {
        List<DeficiencyResponse> deficiencies = r.getDeficiencies().stream()
                .map(DeficiencyResponse::from)
                .toList();
        List<TranscriptCourseResponse> courses = r.getTranscriptCourses().stream()
                .map(TranscriptCourseResponse::from)
                .toList();
        return new AnalysisResultDetailResponse(
                r.getId(),
                r.getJobId(),
                r.getMaskedStudentRef(),
                r.getDepartmentId(),
                r.getStatus().name(),
                r.getIsEligible(),
                r.getTotalCredit(),
                r.getTotalEcts(),
                r.getCreatedAt(),
                r.getCompletedAt(),
                deficiencies,
                courses
        );
    }
}
