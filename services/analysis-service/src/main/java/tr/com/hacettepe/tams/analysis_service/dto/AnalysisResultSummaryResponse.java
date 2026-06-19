package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight result row for paginated list responses (no category or course detail).
 */
@Schema(description = "Summary of a single graduation analysis result (no category or course detail)")
public record AnalysisResultSummaryResponse(
        @Schema(description = "Primary key of this result record")
        UUID id,
        @Schema(description = "Kafka job ID linking the upload to this result")
        String jobId,
        @Schema(description = "SHA-256-derived masked student identifier — no raw PII")
        String maskedStudentRef,
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
        OffsetDateTime completedAt
) {
    public static AnalysisResultSummaryResponse from(AnalysisResult r) {
        return new AnalysisResultSummaryResponse(
                r.getId(),
                r.getJobId(),
                r.getMaskedStudentRef(),
                r.getDepartmentId(),
                r.getDepartmentName(),
                r.getStatus().name(),
                r.getIsEligible(),
                r.getGpa(),
                r.getTotalCredit(),
                r.getTotalEcts(),
                r.getCreatedAt(),
                r.getCompletedAt()
        );
    }
}
