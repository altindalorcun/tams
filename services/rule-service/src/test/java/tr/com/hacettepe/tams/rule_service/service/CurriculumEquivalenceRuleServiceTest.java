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
import tr.com.hacettepe.tams.rule_service.dto.CreateCurriculumEquivalenceRuleRequest;
import tr.com.hacettepe.tams.rule_service.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.rule_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.rule_service.repository.CurriculumEquivalenceRuleRepository;
import tr.com.hacettepe.tams.rule_service.repository.DepartmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CurriculumEquivalenceRuleService}.
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
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("GROUP_MUTUAL without effectiveFromYear — succeeds")
        void create_groupMutual_withoutEffectiveYear_succeeds() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(ruleRepository.save(any(CurriculumEquivalenceRule.class))).thenAnswer(inv -> {
                CurriculumEquivalenceRule saved = inv.getArgument(0);
                saved.setId(RULE_ID);
                return saved;
            });

            CreateCurriculumEquivalenceRuleRequest request = new CreateCurriculumEquivalenceRuleRequest(
                    "GROUP_MUTUAL",
                    List.of("BBM419"),
                    List.of("BBM479", "BBM480"),
                    null,
                    null);

            CurriculumEquivalenceRuleDto result = ruleService.create(DEPT_ID, request);

            assertThat(result.ruleType()).isEqualTo("GROUP_MUTUAL");
            assertThat(result.effectiveFromYear()).isNull();
            assertThat(result.legacyCourseCodes()).containsExactly("BBM419");
            assertThat(result.replacementCourseCodes()).containsExactly("BBM479", "BBM480");
        }

        @Test
        @DisplayName("GROUP_LEGACY_TO_REPLACEMENT without effectiveFromYear — succeeds")
        void create_groupLegacy_withoutEffectiveYear_succeeds() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(ruleRepository.save(any(CurriculumEquivalenceRule.class))).thenAnswer(inv -> {
                CurriculumEquivalenceRule saved = inv.getArgument(0);
                saved.setId(RULE_ID);
                return saved;
            });

            CreateCurriculumEquivalenceRuleRequest request = new CreateCurriculumEquivalenceRuleRequest(
                    "GROUP_LEGACY_TO_REPLACEMENT",
                    List.of("FIZ103", "FIZ104"),
                    List.of("FIZ117"),
                    null,
                    null);

            CurriculumEquivalenceRuleDto result = ruleService.create(DEPT_ID, request);

            assertThat(result.ruleType()).isEqualTo("GROUP_LEGACY_TO_REPLACEMENT");
            assertThat(result.effectiveFromYear()).isNull();
        }
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
