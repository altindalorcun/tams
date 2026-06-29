package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.GlobalCheckResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Read-only representation of a department-level global graduation rule evaluation.
 */
@Schema(description = "Evaluation outcome for a department-level global rule (total ECTS or fail-grade block)")
public record GlobalCheckResultResponse(
        @Schema(description = "Type of global rule evaluated", allowableValues = {"TOTAL_ECTS", "FAIL_GRADE"})
        String checkType,
        @Schema(description = "True when the student satisfies this global rule")
        boolean passed,
        @Schema(description = "Minimum total ECTS required by the department; present for TOTAL_ECTS checks")
        BigDecimal requiredMinEcts,
        @Schema(description = "Total earned ECTS from passed transcript courses; present for TOTAL_ECTS checks")
        BigDecimal earnedEcts,
        @Schema(description = "Course codes with failing grades; present for FAIL_GRADE checks")
        List<String> failedCourseCodes
) {
    /**
     * Maps a persisted {@link GlobalCheckResult} entity to this response record.
     */
    public static GlobalCheckResultResponse from(GlobalCheckResult entity) {
        List<String> failedCodes = entity.getFailedCourseCodes() != null
                ? Arrays.asList(entity.getFailedCourseCodes())
                : List.of();
        return new GlobalCheckResultResponse(
                entity.getCheckType(),
                entity.isPassed(),
                entity.getRequiredMinEcts(),
                entity.getEarnedEcts(),
                failedCodes
        );
    }
}
