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
import tr.com.hacettepe.tams.rule_service.dto.CreateCurriculumEquivalenceRuleRequest;
import tr.com.hacettepe.tams.rule_service.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.rule_service.service.CurriculumEquivalenceRuleService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for curriculum equivalence rule management.
 * These rules capture course replacements that arose from curriculum changes
 * (e.g. HAS222↔MUH103, BBM419↔BBM479+BBM480). All endpoints require ADMIN role.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Curriculum Equivalence Rules", description = "Course equivalence rule management for curriculum changes")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumEquivalenceRuleController {

    private final CurriculumEquivalenceRuleService ruleService;

    @PostMapping("/api/v1/departments/{departmentId}/curriculum-equivalence-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a curriculum equivalence rule for a department")
    public ResponseEntity<CurriculumEquivalenceRuleDto> create(
            @PathVariable UUID departmentId,
            @Valid @RequestBody CreateCurriculumEquivalenceRuleRequest request) {
        CurriculumEquivalenceRuleDto created = ruleService.create(departmentId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/departments/{departmentId}/curriculum-equivalence-rules/{id}")
                .buildAndExpand(departmentId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/v1/departments/{departmentId}/curriculum-equivalence-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all curriculum equivalence rules for a department")
    public ResponseEntity<List<CurriculumEquivalenceRuleDto>> findByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(ruleService.findByDepartment(departmentId));
    }

    @DeleteMapping("/api/v1/departments/{departmentId}/curriculum-equivalence-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a curriculum equivalence rule by ID within a department")
    public ResponseEntity<Void> delete(@PathVariable UUID departmentId, @PathVariable UUID id) {
        ruleService.delete(departmentId, id);
        return ResponseEntity.noContent().build();
    }
}
