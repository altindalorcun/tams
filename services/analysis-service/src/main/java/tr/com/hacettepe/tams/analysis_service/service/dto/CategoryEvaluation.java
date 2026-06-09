package tr.com.hacettepe.tams.analysis_service.service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Evaluation outcome for a single graduation requirement category.
 * Produced by {@link tr.com.hacettepe.tams.analysis_service.service.GraduationEngine}
 * and used to build {@link tr.com.hacettepe.tams.analysis_service.domain.Deficiency} rows.
 *
 * @param categoryId             UUID of the category in rule-service
 * @param categoryName           display name of the category
 * @param satisfied              true only when all thresholds and mandatory courses are met
 * @param requiredCredit         minimum credit threshold from the rule set
 * @param earnedCredit           total credit the student actually earned in this category
 * @param requiredEcts           minimum ECTS threshold from the rule set
 * @param earnedEcts             total ECTS the student actually earned in this category
 * @param requiredCourseCount    minimum course count threshold from the rule set
 * @param earnedCourseCount      number of passed courses in this category's pool
 * @param missingMandatoryCourses course codes that are mandatory but not yet passed
 */
public record CategoryEvaluation(
        UUID categoryId,
        String categoryName,
        boolean satisfied,
        BigDecimal requiredCredit,
        BigDecimal earnedCredit,
        BigDecimal requiredEcts,
        BigDecimal earnedEcts,
        int requiredCourseCount,
        int earnedCourseCount,
        List<String> missingMandatoryCourses
) {}
