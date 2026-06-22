package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.util.UUID;

/**
 * Mirror of the rule-service {@code PrefixLimitDto}.
 * Caps how many courses whose code starts with {@code courseCodePrefix}
 * can be counted towards the parent category's thresholds.
 */
public record PrefixLimitDto(
        UUID id,
        String courseCodePrefix,
        int maxCount
) {}
