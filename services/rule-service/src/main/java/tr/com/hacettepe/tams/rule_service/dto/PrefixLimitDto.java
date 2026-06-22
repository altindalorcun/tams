package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.CategoryPrefixLimit;

import java.util.UUID;

/**
 * Read-only representation of a category prefix limit.
 * Courses whose code starts with {@code courseCodePrefix} are counted at most
 * {@code maxCount} times when evaluating the category's thresholds.
 */
public record PrefixLimitDto(
        UUID id,
        String courseCodePrefix,
        int maxCount
) {
    public static PrefixLimitDto from(CategoryPrefixLimit limit) {
        return new PrefixLimitDto(limit.getId(), limit.getCourseCodePrefix(), limit.getMaxCount());
    }
}
