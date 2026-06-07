package tr.com.hacettepe.tams.rule_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.com.hacettepe.tams.rule_service.dto.RuleSetResponse;
import tr.com.hacettepe.tams.rule_service.service.CategoryService;

import java.util.UUID;

/**
 * Internal endpoint consumed exclusively by analysis-service.
 * Not exposed through the api-gateway; accessible only within the cluster network.
 * No JWT required — callers are trusted internal services.
 */
@RestController
@RequestMapping("/internal/rules")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Internal endpoints for service-to-service communication")
public class InternalRulesController {

    private final CategoryService categoryService;

    @GetMapping("/{departmentId}")
    @Operation(summary = "Fetch the full graduation rule set for a department (internal use only)")
    public ResponseEntity<RuleSetResponse> getRuleSet(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(categoryService.getRuleSet(departmentId));
    }
}
