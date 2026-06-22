package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Mirror of the rule-service {@code ExemptionRuleDto}.
 * When all {@code requiredCourseCodes} are present in the student's passed courses,
 * the engine adds {@code exemptedCourseCode} to the effective passed set.
 */
public record ExemptionRuleDto(
        UUID id,
        List<String> requiredCourseCodes,
        String exemptedCourseCode
) {}
