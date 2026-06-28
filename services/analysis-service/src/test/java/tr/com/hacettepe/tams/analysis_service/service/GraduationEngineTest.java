package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.analysis_service.client.dto.CurriculumEquivalenceRuleDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCategoryDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleCourseDto;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedSemester;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedTranscriptMessage;
import tr.com.hacettepe.tams.analysis_service.service.dto.CategoryEvaluation;
import tr.com.hacettepe.tams.analysis_service.service.dto.EngineResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GraduationEngine}.
 * Tests are written against expected behavior, not implementation details.
 */
class GraduationEngineTest {

    private GraduationEngine engine;

    @BeforeEach
    void setUp() {
        AcademicYearParser academicYearParser = new AcademicYearParser();
        engine = new GraduationEngine(
                new GpaCalculator(),
                new EnrollmentYearParser(),
                new CurriculumEquivalenceExpander(academicYearParser));
    }

    // ── Helper factories ──────────────────────────────────────────────────────

    private ParsedCourse course(String code, double credit, double ects, boolean passed) {
        return new ParsedCourse(code, "Course " + code, credit, ects, passed ? "AA" : "FF", "23-24", passed);
    }

    private RuleCourseDto ruleCourse(String code, double credit, double ects, boolean mandatory) {
        return new RuleCourseDto(code, "Course " + code,
                BigDecimal.valueOf(credit), BigDecimal.valueOf(ects), mandatory, null, null);
    }

    private RuleCategoryDto category(String name, double minCredit, double minEcts,
                                     int minCount, List<RuleCourseDto> courses) {
        return new RuleCategoryDto(UUID.randomUUID(), name,
                BigDecimal.valueOf(minCredit), BigDecimal.valueOf(minEcts), minCount,
                null, null, null, null, null, courses, List.of());
    }

    private ParsedTranscriptMessage transcript(List<ParsedCourse> courses) {
        return new ParsedTranscriptMessage(
                "21627208", "job-1", "teacher-1", "dept-1",
                List.of(new ParsedSemester("Fall 2023", courses)), null);
    }

    private RuleSetResponse ruleSet(List<RuleCategoryDto> categories) {
        return new RuleSetResponse(UUID.randomUUID(), "Computer Engineering", null, false, categories, List.of());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void evaluate_returnsEligible_whenAllCategoriesSatisfied() {
        var courses = List.of(
                course("BBM101", 3, 6, true),
                course("BBM201", 3, 6, true)
        );
        var category = category("Mandatory", 6, 12, 2, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM201", 3, 6, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isTrue();
        assertThat(result.categoryEvaluations()).hasSize(1);
        assertThat(result.categoryEvaluations().get(0).satisfied()).isTrue();
    }

    @Test
    void evaluate_returnsNotEligible_whenCreditDeficit() {
        var courses = List.of(
                course("BBM101", 2, 4, true)   // 2 credit, but 4 required
        );
        var category = category("Mandatory", 4, 0, 0, List.of(
                ruleCourse("BBM101", 2, 4, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isFalse();
        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.satisfied()).isFalse();
        assertThat(eval.earnedCredit()).isEqualByComparingTo("2");
        assertThat(eval.requiredCredit()).isEqualByComparingTo("4");
    }

    @Test
    void evaluate_returnsNotEligible_whenEctsDeficit() {
        var courses = List.of(
                course("BBM101", 3, 5, true)   // 5 ECTS earned, 10 required
        );
        var category = category("Mandatory", 0, 10, 0, List.of(
                ruleCourse("BBM101", 3, 5, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isFalse();
        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.satisfied()).isFalse();
        assertThat(eval.earnedEcts()).isEqualByComparingTo("5");
        assertThat(eval.requiredEcts()).isEqualByComparingTo("10");
    }

    @Test
    void evaluate_returnsNotEligible_whenCourseCountDeficit() {
        var courses = List.of(
                course("BBM101", 3, 6, true)   // 1 course passed, 3 required
        );
        var category = category("Technical Elective", 0, 0, 3, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM201", 3, 6, false),
                ruleCourse("BBM301", 3, 6, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isFalse();
        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.satisfied()).isFalse();
        assertThat(eval.earnedCourseCount()).isEqualTo(1);
        assertThat(eval.requiredCourseCount()).isEqualTo(3);
    }

    @Test
    void evaluate_returnsNotEligible_whenMandatoryCourseNotPassed() {
        var courses = List.of(
                course("BBM101", 3, 6, true),
                course("BBM202", 3, 6, false)  // mandatory but NOT passed
        );
        var category = category("Mandatory", 3, 6, 1, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM202", 3, 6, true)  // is_mandatory = true
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isFalse();
        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.satisfied()).isFalse();
        assertThat(eval.missingMandatoryCourses()).containsExactly("BBM202");
    }

    @Test
    void evaluate_categoryStillSatisfied_whenMandatoryCourseIsPassed() {
        var courses = List.of(
                course("BBM101", 3, 6, true),
                course("BBM202", 3, 6, true)   // mandatory AND passed
        );
        var category = category("Mandatory", 6, 12, 2, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM202", 3, 6, true)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isTrue();
        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.satisfied()).isTrue();
        assertThat(eval.missingMandatoryCourses()).isEmpty();
    }

    @Test
    void evaluate_returnsNotEligible_whenMultipleCategoriesAreDeficient() {
        var courses = List.of(
                course("BBM101", 2, 4, true)  // only one course passed in total
        );
        var cat1 = category("Category A", 4, 0, 2, List.of(
                ruleCourse("BBM101", 2, 4, false),
                ruleCourse("BBM201", 2, 4, false)
        ));
        var cat2 = category("Category B", 6, 0, 2, List.of(
                ruleCourse("BBM301", 3, 6, true),
                ruleCourse("BBM401", 3, 6, true)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(cat1, cat2)));

        assertThat(result.eligible()).isFalse();
        long deficientCount = result.categoryEvaluations().stream()
                .filter(e -> !e.satisfied())
                .count();
        assertThat(deficientCount).isEqualTo(2);
    }

    @Test
    void evaluate_returnsEligible_whenRuleSetHasNoCategories() {
        var courses = List.of(course("BBM101", 3, 6, true));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of()));

        assertThat(result.eligible()).isTrue();
        assertThat(result.categoryEvaluations()).isEmpty();
    }

    @Test
    void evaluate_courseCodeMatchingIsCaseInsensitive() {
        // transcript uses lowercase, rule uses uppercase
        var courses = List.of(course("bbm101", 3, 6, true));
        var category = category("Mandatory", 3, 6, 1, List.of(
                ruleCourse("BBM101", 3, 6, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        assertThat(result.eligible()).isTrue();
        assertThat(result.categoryEvaluations().get(0).earnedCourseCount()).isEqualTo(1);
    }

    @Test
    void evaluate_failedCourseIsNotCountedInCategory() {
        // Student attempted BBM101 but did not pass it
        var courses = List.of(
                course("BBM101", 3, 6, false),  // not passed
                course("BBM201", 3, 6, true)    // passed
        );
        var category = category("Elective", 6, 12, 2, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM201", 3, 6, false)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.earnedCourseCount()).isEqualTo(1);
        assertThat(eval.earnedCredit()).isEqualByComparingTo("3");
        assertThat(result.eligible()).isFalse();  // only 1/2 courses passed
    }

    @Test
    void evaluate_totalCreditAndEctsAreAcrossAllPassedCourses() {
        // Two semesters; only passed courses should sum
        var semester1 = new ParsedSemester("Fall 2022", List.of(
                course("BBM101", 3, 6, true),
                course("BBM102", 2, 4, false)   // failed — excluded from totals
        ));
        var semester2 = new ParsedSemester("Spring 2023", List.of(
                course("BBM201", 4, 8, true),
                course("BBM202", 3, 6, true)
        ));
        var msg = new ParsedTranscriptMessage("21627208", "job-1", "t-1", "d-1",
                List.of(semester1, semester2), null);

        EngineResult result = engine.evaluate(msg, ruleSet(List.of()));

        // Only BBM101(3cr/6ects) + BBM201(4cr/8ects) + BBM202(3cr/6ects) should count
        assertThat(result.totalCredit()).isEqualByComparingTo("10");
        assertThat(result.totalEcts()).isEqualByComparingTo("20");
    }

    @Test
    void evaluate_missingMandatoryListIsAccurate_whenMultipleMandatoryCoursesAreMissing() {
        var courses = List.of(course("BBM101", 3, 6, true));  // BBM202, BBM303 not passed
        var category = category("Core", 3, 6, 1, List.of(
                ruleCourse("BBM101", 3, 6, false),
                ruleCourse("BBM202", 3, 6, true),
                ruleCourse("BBM303", 3, 6, true)
        ));

        EngineResult result = engine.evaluate(transcript(courses), ruleSet(List.of(category)));

        CategoryEvaluation eval = result.categoryEvaluations().get(0);
        assertThat(eval.missingMandatoryCourses()).containsExactlyInAnyOrder("BBM202", "BBM303");
        assertThat(result.eligible()).isFalse();
    }
}
