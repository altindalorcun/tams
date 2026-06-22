package tr.com.hacettepe.tams.rule_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tr.com.hacettepe.tams.rule_service.dto.CreateExemptionRuleRequest;
import tr.com.hacettepe.tams.rule_service.dto.ExemptionRuleDto;
import tr.com.hacettepe.tams.rule_service.service.ExemptionRuleService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for exemption rule management.
 * Exemption rules define course substitutions: passing a set of courses grants
 * implicit credit for another course. All endpoints require ADMIN role.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Exemption Rules", description = "Course exemption rule management per department")
@SecurityRequirement(name = "bearerAuth")
public class ExemptionRuleController {

    private final ExemptionRuleService exemptionRuleService;

    @PostMapping("/api/v1/departments/{departmentId}/exemption-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an exemption rule for a department")
    public ResponseEntity<ExemptionRuleDto> create(@PathVariable UUID departmentId,
                                                   @Valid @RequestBody CreateExemptionRuleRequest request) {
        ExemptionRuleDto created = exemptionRuleService.create(departmentId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/departments/{departmentId}/exemption-rules/{id}")
                .buildAndExpand(departmentId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/v1/departments/{departmentId}/exemption-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all exemption rules for a department")
    public ResponseEntity<List<ExemptionRuleDto>> findByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(exemptionRuleService.findByDepartment(departmentId));
    }

    @DeleteMapping("/api/v1/exemption-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an exemption rule by ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        exemptionRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
