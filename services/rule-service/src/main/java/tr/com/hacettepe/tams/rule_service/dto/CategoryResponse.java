package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Response body representing a graduation category (without its course list). */
public record CategoryResponse(
        UUID id,
        UUID departmentId,
        String name,
        String description,
        BigDecimal minCredit,
        BigDecimal minEcts,
        int minCourseCount,
        Integer appliesFromYear,
        Integer appliesToYear,
        List<String> conditionCourseCodes,
        Integer minCourseCountIfMet,
        BigDecimal minEctsIfMet,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PrefixLimitDto> prefixLimits
) {
    public static CategoryResponse from(Category c) {
        List<String> conditionCodes = c.getConditionCourseCodes() != null
                ? Arrays.asList(c.getConditionCourseCodes())
                : List.of();
        List<PrefixLimitDto> prefixLimits = c.getPrefixLimits().stream()
                .map(PrefixLimitDto::from)
                .toList();
        return new CategoryResponse(
                c.getId(),
                c.getDepartment().getId(),
                c.getName(),
                c.getDescription(),
                c.getMinCredit(),
                c.getMinEcts(),
                c.getMinCourseCount(),
                c.getAppliesFromYear(),
                c.getAppliesToYear(),
                conditionCodes,
                c.getMinCourseCountIfMet(),
                c.getMinEctsIfMet(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                prefixLimits
        );
    }
}
