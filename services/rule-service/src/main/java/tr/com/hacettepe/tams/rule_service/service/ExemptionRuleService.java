package tr.com.hacettepe.tams.rule_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.domain.ExemptionRule;
import tr.com.hacettepe.tams.rule_service.dto.CreateExemptionRuleRequest;
import tr.com.hacettepe.tams.rule_service.dto.ExemptionRuleDto;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;
import tr.com.hacettepe.tams.rule_service.repository.ExemptionRuleRepository;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for exemption rule management.
 * An exemption rule grants a student implicit credit for a course they haven't taken
 * when they have passed all prerequisite courses in {@code requiredCourseCodes}.
 */
@Service
@RequiredArgsConstructor
public class ExemptionRuleService {

    private final ExemptionRuleRepository exemptionRuleRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public ExemptionRuleDto create(UUID departmentId, CreateExemptionRuleRequest request) {
        Department department = getDepartmentOrThrow(departmentId);
        String[] requiredCodes = request.requiredCourseCodes().stream()
                .map(String::toUpperCase)
                .toArray(String[]::new);
        ExemptionRule rule = new ExemptionRule(
                department,
                requiredCodes,
                request.exemptedCourseCode().toUpperCase()
        );
        return ExemptionRuleDto.from(exemptionRuleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<ExemptionRuleDto> findByDepartment(UUID departmentId) {
        getDepartmentOrThrow(departmentId);
        return exemptionRuleRepository.findByDepartmentId(departmentId).stream()
                .map(ExemptionRuleDto::from)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        ExemptionRule rule = exemptionRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exemption rule not found: " + id));
        exemptionRuleRepository.delete(rule);
    }

    private Department getDepartmentOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }
}
