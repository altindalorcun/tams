package tr.com.hacettepe.tams.analysis_service.service;

import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.analysis_service.dto.kafka.ParsedCourse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Computes the cumulative GPA using the Hacettepe University 4.00 grading scale.
 *
 * <p>Formula: GPA = Σ(credit_i × grade_point_i) / Σ(credit_i)
 * Only courses with a recognized grade code contribute to the calculation.
 * Unrecognized codes (e.g. "G", "EX", "W") are silently skipped.
 */
@Component
public class GpaCalculator {

    private static final Map<String, BigDecimal> GRADE_POINTS = Map.ofEntries(
            Map.entry("A1", new BigDecimal("4.00")),
            Map.entry("A2", new BigDecimal("3.75")),
            Map.entry("A3", new BigDecimal("3.50")),
            Map.entry("B1", new BigDecimal("3.25")),
            Map.entry("B2", new BigDecimal("3.00")),
            Map.entry("B3", new BigDecimal("2.75")),
            Map.entry("C1", new BigDecimal("2.50")),
            Map.entry("C2", new BigDecimal("2.25")),
            Map.entry("C3", new BigDecimal("2.00")),
            Map.entry("D",  new BigDecimal("1.75")),
            Map.entry("F",  new BigDecimal("0.00"))
    );

    /**
     * Calculates GPA from a flat list of transcript courses.
     *
     * @param courses all courses across all semesters
     * @return GPA rounded to 2 decimal places, or {@link BigDecimal#ZERO} when no graded courses exist
     */
    public BigDecimal calculate(List<ParsedCourse> courses) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ParsedCourse course : courses) {
            if (course.grade() == null) {
                continue;
            }
            BigDecimal gradePoint = GRADE_POINTS.get(course.grade().trim().toUpperCase());
            if (gradePoint == null) {
                continue;
            }
            BigDecimal credit = BigDecimal.valueOf(course.credit());
            weightedSum = weightedSum.add(credit.multiply(gradePoint));
            totalCredit = totalCredit.add(credit);
        }

        if (totalCredit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return weightedSum.divide(totalCredit, 2, RoundingMode.HALF_UP);
    }
}
