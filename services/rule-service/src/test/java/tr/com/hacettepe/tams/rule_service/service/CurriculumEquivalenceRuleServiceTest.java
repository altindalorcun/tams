package tr.com.hacettepe.tams.rule_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.hacettepe.tams.rule_service.domain.CurriculumEquivalenceRule;
import tr.com.hacettepe.tams.rule_service.domain.Department;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CurriculumEquivalenceRuleRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CurriculumEquivalenceRuleService} delete behaviour.
 */
@ExtendWith(MockitoExtension.class)
class CurriculumEquivalenceRuleServiceTest {

    @Mock private CurriculumEquivalenceRuleRepository ruleRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks private CurriculumEquivalenceRuleService ruleService;

    private Department department;
    private CurriculumEquivalenceRule rule;

    private final UUID DEPT_ID = UUID.randomUUID();
    private final UUID RULE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        department = new Department("Bilgisayar Mühendisliği", "BBM", null);
        department.setId(DEPT_ID);
        rule = new CurriculumEquivalenceRule(
                department, "PAIRWISE", new String[] {"HAS222"}, new String[] {"MUH103"}, null, null);
        rule.setId(RULE_ID);
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("happy path — deletes the rule within the department")
        void delete_happyPath() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(ruleRepository.findByIdAndDepartmentId(RULE_ID, DEPT_ID)).thenReturn(Optional.of(rule));

            ruleService.delete(DEPT_ID, RULE_ID);

            verify(ruleRepository).delete(rule);
        }

        @Test
        @DisplayName("rule not found in department — throws ResourceNotFoundException")
        void delete_notFound_throws() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(ruleRepository.findByIdAndDepartmentId(RULE_ID, DEPT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ruleService.delete(DEPT_ID, RULE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(ruleRepository, never()).delete(any());
        }
    }
}
