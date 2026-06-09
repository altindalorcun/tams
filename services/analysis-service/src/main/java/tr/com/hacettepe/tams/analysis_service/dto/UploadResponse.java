package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a successful transcript upload.
 * The caller should poll {@code GET /api/v1/transcripts/{jobId}/status} to track progress.
 */
@Schema(description = "Acknowledgement returned immediately after a transcript upload is accepted")
public record UploadResponse(
        @Schema(description = "UUID of the created analysis job", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String jobId,
        @Schema(description = "Initial status — always PENDING at upload time", example = "PENDING")
        String status
) {}
