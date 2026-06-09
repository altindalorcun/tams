package tr.com.hacettepe.tams.analysis_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultDetailResponse;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultSummaryResponse;
import tr.com.hacettepe.tams.analysis_service.service.ResultQueryService;

import java.util.UUID;

/**
 * Exposes read-only result query endpoints for teachers and students.
 *
 * <p>Role enforcement:
 * <ul>
 *   <li>TEACHER — list view and per-result detail; results filtered to their own uploads.</li>
 *   <li>STUDENT — own result only, identified by their masked student reference.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Tag(name = "Results", description = "Query graduation analysis results")
public class ResultController {

    private final ResultQueryService resultQueryService;

    @Operation(
            summary = "List analysis results for the authenticated teacher",
            description = "Returns a paginated list of result summaries. Optionally filter by partial masked student reference.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page of results",
                            content = @Content(schema = @Schema(implementation = AnalysisResultSummaryResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not a TEACHER",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<AnalysisResultSummaryResponse>> listResults(
            @Parameter(description = "Optional partial masked student reference to search by")
            @RequestParam(required = false) String studentRef,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        UUID teacherId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(resultQueryService.listForTeacher(teacherId, studentRef, pageable));
    }

    @Operation(
            summary = "Get full result detail by ID",
            description = "Returns the complete result including deficiency breakdown and course snapshot. " +
                    "TEACHER: accessible only for own uploads. " +
                    "STUDENT: must supply the matching studentRef query parameter.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Result detail",
                            content = @Content(schema = @Schema(implementation = AnalysisResultDetailResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not the owner of this result",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Result not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<AnalysisResultDetailResponse> getResult(
            @PathVariable UUID id,
            @Parameter(description = "Required when caller is a STUDENT: the caller's masked student reference")
            @RequestParam(required = false) String studentRef,
            Authentication authentication) {

        String role = extractRole(authentication);
        UUID callerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(resultQueryService.getById(id, role, callerId, studentRef));
    }

    @Operation(
            summary = "Get the authenticated student's latest result",
            description = "Returns the most recent analysis result for the student identified by the supplied masked student reference.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Latest result detail",
                            content = @Content(schema = @Schema(implementation = AnalysisResultDetailResponse.class))),
                    @ApiResponse(responseCode = "400", description = "studentRef parameter is required",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not a STUDENT",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "No results found for this student reference",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AnalysisResultDetailResponse> getMyResult(
            @Parameter(description = "The student's masked reference (SHA-256 hash of TC + Öğrenci No)")
            @RequestParam String studentRef) {

        return ResponseEntity.ok(resultQueryService.getLatestForStudent(studentRef));
    }

    /**
     * Extracts the role string from the granted authority set, stripping the {@code ROLE_} prefix.
     * Returns {@code UNKNOWN} when no matching authority is found.
     */
    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("UNKNOWN");
    }
}
