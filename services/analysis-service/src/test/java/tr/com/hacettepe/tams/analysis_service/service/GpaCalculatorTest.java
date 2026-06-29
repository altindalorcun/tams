package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.Test;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GpaCalculator}.
 */
class GpaCalculatorTest {

    private final GpaCalculator calculator = new GpaCalculator();

    @Test
    void calculate_returnsWeightedAverage_forRecognizedGrades() {
        var courses = List.of(
                new ParsedCourse("BBM101", "Intro", 3, 6, "A1", "23-24", true),
                new ParsedCourse("BBM201", "Data", 3, 6, "B2", "23-24", true)
        );

        BigDecimal gpa = calculator.calculate(courses);

        assertThat(gpa).isEqualByComparingTo("3.50");
    }

    @Test
    void calculate_skipsUnrecognizedGrades() {
        var courses = List.of(
                new ParsedCourse("BBM101", "Intro", 3, 6, "A1", "23-24", true),
                new ParsedCourse("BYZ695", "Thesis", 0, 10, "E", "23-24", false)
        );

        BigDecimal gpa = calculator.calculate(courses);

        assertThat(gpa).isEqualByComparingTo("4.00");
    }

    @Test
    void calculate_returnsZero_whenNoGradedCoursesExist() {
        assertThat(calculator.calculate(List.of())).isEqualByComparingTo("0.00");
    }
}
