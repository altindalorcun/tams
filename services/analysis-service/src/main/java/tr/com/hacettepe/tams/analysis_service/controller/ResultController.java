package tr.com.hacettepe.tams.analysis_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultDetailResponse;
import tr.com.hacettepe.tams.analysis_service.dto.AnalysisResultSummaryResponse;
import tr.com.hacettepe.tams.analysis_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.analysis_service.security.JwtUtil;
import tr.com.hacettepe.tams.analysis_service.service.ResultQueryService;

import java.util.UUID;

/**
 * Exposes read-only result query endpoints for teachers and students.
 *
 * <p>Role enforcement:
 * <ul>
 *   <li>TEACHER — list view, per-result detail, and by-job lookup; results filtered to their uploads.</li>
 *   <li>STUDENT — own result only, identified via JWT {@code studentNumber} claim (no query param required).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Tag(name = "Results", description = "Query graduation analysis results")
public class ResultController {

    private final ResultQueryService resultQueryService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "List analysis results for the authenticated teacher",
            description = "Returns a paginated list of result summaries. Optionally filter by partial student number.",
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
            @Parameter(description = "Optional partial student number to search by")
            @RequestParam(required = false) String studentNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        UUID teacherId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(resultQueryService.listForTeacher(teacherId, studentNumber, pageable));
    }

    @Operation(
            summary = "Get full result detail by ID",
            description = "TEACHER: accessible only for own uploads. STUDENT: accessible only for results matching their student number.",
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
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String role = extractRole(authentication);
        UUID callerId = UUID.fromString(authentication.getName());
        String studentNumber = "STUDENT".equals(role)
                ? extractStudentNumber(httpRequest)
                : null;
        return ResponseEntity.ok(resultQueryService.getById(id, role, callerId, studentNumber));
    }

    @Operation(
            summary = "Get full result by job ID",
            description = "Allows a teacher to retrieve the analysis result using the jobId returned at upload time.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Result detail",
                            content = @Content(schema = @Schema(implementation = AnalysisResultDetailResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not a TEACHER",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Result not found for given jobId",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/by-job/{jobId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnalysisResultDetailResponse> getResultByJobId(@PathVariable String jobId) {
        return ResponseEntity.ok(resultQueryService.getByJobId(jobId));
    }

    @Operation(
            summary = "Get the authenticated student's latest result",
            description = "Extracts the student number from the JWT claim and returns the most recent analysis result. No query parameter required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Latest result detail",
                            content = @Content(schema = @Schema(implementation = AnalysisResultDetailResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated or studentNumber claim missing",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "403", description = "Not a STUDENT",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "No results found for this student",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AnalysisResultDetailResponse> getMyResult(HttpServletRequest httpRequest) {
        String studentNumber = extractStudentNumber(httpRequest);
        return ResponseEntity.ok(resultQueryService.getLatestForStudent(studentNumber));
    }

    private String extractStudentNumber(HttpServletRequest request) {
        String token = extractBearerToken(request);
        String studentNumber = jwtUtil.extractStudentNumber(token);
        if (!StringUtils.hasText(studentNumber)) {
            throw new UnauthorizedException("JWT does not contain a studentNumber claim");
        }
        return studentNumber;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new UnauthorizedException("Authorization header is missing or malformed");
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("UNKNOWN");
    }
}
