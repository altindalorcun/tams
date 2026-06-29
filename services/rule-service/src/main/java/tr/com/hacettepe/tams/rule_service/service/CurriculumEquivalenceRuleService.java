package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.CurriculumEquivalenceRule;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.dto.CreateCurriculumEquivalenceRuleRequest;
import tr.com.hacettepe.tams.rule_service.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CurriculumEquivalenceRuleRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic for curriculum equivalence rule management.
 *
 * <p>Validates that:
 * <ul>
 *   <li>PAIRWISE rules have equal-length legacy and replacement lists.</li>
 *   <li>GROUP rules have at least one course in each list; effective year is optional
 *       (when omitted the analysis engine skips the completion-date check).</li>
 *   <li>All course codes are normalised to upper-case.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CurriculumEquivalenceRuleService {

    private static final Set<String> VALID_RULE_TYPES =
            Set.of("PAIRWISE", "GROUP_LEGACY_TO_REPLACEMENT", "GROUP_REPLACEMENT_TO_LEGACY", "GROUP_MUTUAL");

    private final CurriculumEquivalenceRuleRepository ruleRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public CurriculumEquivalenceRuleDto create(UUID departmentId, CreateCurriculumEquivalenceRuleRequest request) {
        Department department = getDepartmentOrThrow(departmentId);
        validateRequest(request);

        String ruleType = request.ruleType().toUpperCase();
        String[] legacy = toUpperCaseArray(request.legacyCourseCodes());
        String[] replacement = toUpperCaseArray(request.replacementCourseCodes());
        String term = request.effectiveFromTerm() != null ? request.effectiveFromTerm().toUpperCase() : null;

        CurriculumEquivalenceRule rule = new CurriculumEquivalenceRule(
                department, ruleType, legacy, replacement, request.effectiveFromYear(), term);
        return CurriculumEquivalenceRuleDto.from(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<CurriculumEquivalenceRuleDto> findByDepartment(UUID departmentId) {
        getDepartmentOrThrow(departmentId);
        return ruleRepository.findByDepartmentId(departmentId).stream()
                .map(CurriculumEquivalenceRuleDto::from)
                .toList();
    }

    @Transactional
    public void delete(UUID departmentId, UUID id) {
        getDepartmentOrThrow(departmentId);
        CurriculumEquivalenceRule rule = getRuleOrThrow(departmentId, id);
        ruleRepository.delete(rule);
    }

    private CurriculumEquivalenceRule getRuleOrThrow(UUID departmentId, UUID id) {
        return ruleRepository.findByIdAndDepartmentId(id, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Curriculum equivalence rule not found in department: " + id));
    }

    private void validateRequest(CreateCurriculumEquivalenceRuleRequest request) {
        String ruleType = request.ruleType() != null ? request.ruleType().toUpperCase() : "";
        if (!VALID_RULE_TYPES.contains(ruleType)) {
            throw new IllegalArgumentException("Invalid rule type: " + request.ruleType()
                    + ". Must be one of: " + VALID_RULE_TYPES);
        }

        int legacySize = request.legacyCourseCodes() != null ? request.legacyCourseCodes().size() : 0;
        int replacementSize = request.replacementCourseCodes() != null ? request.replacementCourseCodes().size() : 0;

        if ("PAIRWISE".equals(ruleType)) {
            if (legacySize != replacementSize || legacySize == 0) {
                throw new IllegalArgumentException(
                        "PAIRWISE rules require equal-length, non-empty legacy and replacement course lists");
            }
        } else {
            if (legacySize == 0 || replacementSize == 0) {
                throw new IllegalArgumentException("GROUP rules require at least one course in each list");
            }
        }
    }

    private Department getDepartmentOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private String[] toUpperCaseArray(List<String> codes) {
        if (codes == null) return new String[0];
        return codes.stream().map(String::toUpperCase).toArray(String[]::new);
    }
}
