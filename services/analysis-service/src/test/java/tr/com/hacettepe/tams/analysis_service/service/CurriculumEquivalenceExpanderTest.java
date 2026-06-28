package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.analysis_service.client.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CurriculumEquivalenceExpander}.
 * Covers PAIRWISE, GROUP_LEGACY_TO_REPLACEMENT, GROUP_REPLACEMENT_TO_LEGACY,
 * GROUP_MUTUAL, fixpoint chaining, and effective-date boundary logic.
 */
class CurriculumEquivalenceExpanderTest {

    private CurriculumEquivalenceExpander expander;

    @BeforeEach
    void setUp() {
        expander = new CurriculumEquivalenceExpander(new AcademicYearParser());
    }

    // ── Helper factories ──────────────────────────────────────────────────────

    private ParsedCourse passed(String code, String academicYear) {
        return new ParsedCourse(code.toUpperCase(), "Course " + code, 3, 6, "AA", academicYear, true);
    }

    private Map<String, ParsedCourse> passedMap(ParsedCourse... courses) {
        java.util.Map<String, ParsedCourse> map = new java.util.LinkedHashMap<>();
        for (ParsedCourse c : courses) {
            map.put(c.courseCode().toUpperCase(), c);
        }
        return map;
    }

    private CurriculumEquivalenceRuleDto pairwise(List<String> legacy, List<String> replacement) {
        return new CurriculumEquivalenceRuleDto(UUID.randomUUID(), "PAIRWISE",
                legacy, replacement, null, null);
    }

    private CurriculumEquivalenceRuleDto groupLegacy(List<String> legacy, List<String> replacement,
                                                      Integer effectiveYear, String term) {
        return new CurriculumEquivalenceRuleDto(UUID.randomUUID(), "GROUP_LEGACY_TO_REPLACEMENT",
                legacy, replacement, effectiveYear, term);
    }

    private CurriculumEquivalenceRuleDto groupReplacement(List<String> legacy, List<String> replacement) {
        return new CurriculumEquivalenceRuleDto(UUID.randomUUID(), "GROUP_REPLACEMENT_TO_LEGACY",
                legacy, replacement, null, null);
    }

    private CurriculumEquivalenceRuleDto groupMutual(List<String> legacy, List<String> replacement,
                                                      Integer effectiveYear, String term) {
        return new CurriculumEquivalenceRuleDto(UUID.randomUUID(), "GROUP_MUTUAL",
                legacy, replacement, effectiveYear, term);
    }

    // ── PAIRWISE ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PAIRWISE rules")
    class PairwiseTests {

        @Test
        @DisplayName("HAS/MUH scenario: HAS222 passed → MUH103 inferred")
        void pairwise_legacyPassed_inferReplacement() {
            var rule = pairwise(List.of("HAS222", "HAS223"), List.of("MUH103", "MUH104"));
            var map = passedMap(passed("HAS222", "18-19"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).contains("HAS222", "MUH103");
            assertThat(result).doesNotContain("HAS223", "MUH104");
        }

        @Test
        @DisplayName("HAS/MUH scenario: MUH104 passed → HAS223 inferred")
        void pairwise_replacementPassed_inferLegacy() {
            var rule = pairwise(List.of("HAS222", "HAS223"), List.of("MUH103", "MUH104"));
            var map = passedMap(passed("MUH104", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).contains("MUH104", "HAS223");
            assertThat(result).doesNotContain("HAS222", "MUH103");
        }

        @Test
        @DisplayName("mixed HAS/MUH: HAS222 + MUH104 → HAS223 and MUH103 both inferred")
        void pairwise_mixedLegacyAndReplacement_bothInferred() {
            var rule = pairwise(List.of("HAS222", "HAS223"), List.of("MUH103", "MUH104"));
            // Student took HAS222 in 18-19 (old curriculum) and MUH104 in 20-21 (new curriculum)
            var map = passedMap(passed("HAS222", "18-19"), passed("MUH104", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule));

            // HAS222 → MUH103 inferred; MUH104 → HAS223 inferred
            assertThat(result).containsAll(List.of("HAS222", "HAS223", "MUH103", "MUH104"));
        }

        @Test
        @DisplayName("pairwise: no courses passed → set unchanged")
        void pairwise_nonePass_nothingInferred() {
            var rule = pairwise(List.of("HAS222"), List.of("MUH103"));
            var map = passedMap();

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("pairwise ignores effective date — year of completion is irrelevant")
        void pairwise_ignoresEffectiveDateCompletely() {
            // Even if a PAIRWISE rule had an effective year, it must be ignored
            var rule = new CurriculumEquivalenceRuleDto(UUID.randomUUID(), "PAIRWISE",
                    List.of("HAS222"), List.of("MUH103"), 2019, "GUZ");
            var map = passedMap(passed("HAS222", "20-21")); // passed AFTER effective date

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).contains("MUH103");
        }
    }

    // ── GROUP_LEGACY_TO_REPLACEMENT ───────────────────────────────────────────

    @Nested
    @DisplayName("GROUP_LEGACY_TO_REPLACEMENT rules")
    class GroupLegacyTests {

        @Test
        @DisplayName("FIZ scenario: FIZ103+FIZ104 passed before effective year → FIZ117 inferred")
        void legacyGroup_allLegacyBeforeEffective_inferReplacement() {
            var rule = groupLegacy(List.of("FIZ103", "FIZ104"), List.of("FIZ117"), 2017, "GUZ");
            var map = passedMap(passed("FIZ103", "16-17"), passed("FIZ104", "16-17"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).contains("FIZ103", "FIZ104", "FIZ117");
        }

        @Test
        @DisplayName("FIZ partial: only FIZ103 passed → FIZ117 NOT inferred")
        void legacyGroup_partialLegacy_notInferred() {
            var rule = groupLegacy(List.of("FIZ103", "FIZ104"), List.of("FIZ117"), 2017, "GUZ");
            var map = passedMap(passed("FIZ103", "16-17"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).doesNotContain("FIZ117");
        }

        @Test
        @DisplayName("FIZ legacy after effective year: FIZ103+FIZ104 passed after 2017 → FIZ117 NOT inferred")
        void legacyGroup_legacyAfterEffectiveYear_notInferred() {
            var rule = groupLegacy(List.of("FIZ103", "FIZ104"), List.of("FIZ117"), 2017, "GUZ");
            // Taken in 2018-2019 (after the 2017 GUZ effective boundary)
            var map = passedMap(passed("FIZ103", "18-19"), passed("FIZ104", "18-19"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).doesNotContain("FIZ117");
        }

        @Test
        @DisplayName("BBM legacy→replacement without effective date: all legacy passed → replacement inferred")
        void legacyGroup_noEffectiveDate_allLegacyPassed_inferReplacement() {
            var rule = groupLegacy(List.of("BBM419"), List.of("BBM479", "BBM480"), null, null);
            var map = passedMap(passed("BBM419", "19-20"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).containsAll(List.of("BBM419", "BBM479", "BBM480"));
        }
    }

    // ── GROUP_REPLACEMENT_TO_LEGACY ───────────────────────────────────────────

    @Nested
    @DisplayName("GROUP_REPLACEMENT_TO_LEGACY rules")
    class GroupReplacementTests {

        @Test
        @DisplayName("BBM: BBM479+BBM480 both passed → BBM419 inferred")
        void replacementGroup_allReplacementPassed_inferLegacy() {
            var rule = groupReplacement(List.of("BBM419"), List.of("BBM479", "BBM480"));
            var map = passedMap(passed("BBM479", "20-21"), passed("BBM480", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).containsAll(List.of("BBM419", "BBM479", "BBM480"));
        }

        @Test
        @DisplayName("BBM partial: only BBM479 passed → BBM419 NOT inferred")
        void replacementGroup_partialReplacement_notInferred() {
            var rule = groupReplacement(List.of("BBM419"), List.of("BBM479", "BBM480"));
            var map = passedMap(passed("BBM479", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).doesNotContain("BBM419");
        }
    }

    // ── GROUP_MUTUAL ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GROUP_MUTUAL rules")
    class GroupMutualTests {

        @Test
        @DisplayName("BBM419 passed before effective → BBM479+BBM480 inferred (legacy→replacement direction)")
        void mutual_legacyPassed_inferReplacement() {
            var rule = groupMutual(List.of("BBM419"), List.of("BBM479", "BBM480"), 2020, "GUZ");
            var map = passedMap(passed("BBM419", "19-20"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).containsAll(List.of("BBM419", "BBM479", "BBM480"));
        }

        @Test
        @DisplayName("BBM479+BBM480 passed → BBM419 inferred (replacement→legacy direction)")
        void mutual_replacementPassed_inferLegacy() {
            var rule = groupMutual(List.of("BBM419"), List.of("BBM479", "BBM480"), 2020, "GUZ");
            var map = passedMap(passed("BBM479", "20-21"), passed("BBM480", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule));

            assertThat(result).containsAll(List.of("BBM419", "BBM479", "BBM480"));
        }
    }

    // ── Fixpoint / chaining ───────────────────────────────────────────────────

    @Nested
    @DisplayName("fixpoint chaining")
    class FixpointTests {

        @Test
        @DisplayName("chain: A→B inferred, then B→C rule fires in second pass")
        void fixpoint_chainedRules_resolveInMultiplePasses() {
            // Rule 1: A passed → B inferred (PAIRWISE)
            var rule1 = pairwise(List.of("COURSE_A"), List.of("COURSE_B"));
            // Rule 2: B passed → C inferred (PAIRWISE)
            var rule2 = pairwise(List.of("COURSE_B"), List.of("COURSE_C"));

            var map = passedMap(passed("COURSE_A", "20-21"));

            Set<String> result = expander.expand(map, List.of(rule1, rule2));

            assertThat(result).containsAll(List.of("COURSE_A", "COURSE_B", "COURSE_C"));
        }

        @Test
        @DisplayName("empty rules list returns original codes only")
        void noRules_returnsOriginalCodes() {
            var map = passedMap(passed("BBM101", "20-21"), passed("BBM201", "21-22"));

            Set<String> result = expander.expand(map, List.of());

            assertThat(result).containsExactlyInAnyOrder("BBM101", "BBM201");
        }

        @Test
        @DisplayName("null rules list returns original codes only")
        void nullRules_returnsOriginalCodes() {
            var map = passedMap(passed("BBM101", "20-21"));

            Set<String> result = expander.expand(map, null);

            assertThat(result).containsExactly("BBM101");
        }
    }
}
