package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for adding a prefix limit to a graduation category.
 * At most {@code maxCount} courses beginning with {@code courseCodePrefix}
 * will be counted towards the category's thresholds.
 */
public record CreatePrefixLimitRequest(
        @NotBlank @Size(max = 10) String courseCodePrefix,
        @Positive int maxCount
) {}
