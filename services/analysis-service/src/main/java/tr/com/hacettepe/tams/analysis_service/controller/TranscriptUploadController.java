package tr.com.hacettepe.tams.analysis_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.dto.UploadResponse;
import tr.com.hacettepe.tams.analysis_service.service.TranscriptService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Accepts PDF transcript uploads from teachers and exposes job status polling.
 */
@RestController
@RequestMapping("/api/v1/transcripts")
@RequiredArgsConstructor
@Tag(name = "Transcripts", description = "Upload transcripts and poll analysis status")
public class TranscriptUploadController {

    private final TranscriptService transcriptService;

    @Operation(
            summary = "Upload a student transcript PDF",
            description = "Accepts a multipart PDF and a departmentId. Publishes to Kafka for async parsing. Returns 202 Accepted with a jobId for polling.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Upload accepted",
                            content = @Content(schema = @Schema(implementation = UploadResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid parameters",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not a TEACHER",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "413", description = "File exceeds 10 MB limit",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<UploadResponse> uploadTranscript(
            @Parameter(description = "PDF transcript file (max 10 MB)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "UUID of the department whose graduation rules should be applied")
            @RequestParam("departmentId") UUID departmentId,
            Authentication authentication) throws IOException {

        UUID teacherId = UUID.fromString(authentication.getName());
        byte[] pdfBytes = file.getBytes();
        UploadResponse response = transcriptService.uploadTranscript(pdfBytes, departmentId, teacherId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "Poll analysis job status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status returned",
                            content = @Content(schema = @Schema(example = "{\"jobId\": \"f47ac10b-...\", \"status\": \"COMPLETED\"}"))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Job not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/{jobId}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getJobStatus(@PathVariable String jobId) {
        AnalysisStatus status = transcriptService.getJobStatus(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", status.name()));
    }
}
