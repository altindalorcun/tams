package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PrefixLimitDto> prefixLimits
) {
    public static CategoryResponse from(Category c) {
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
                c.getCreatedAt(),
                c.getUpdatedAt(),
                prefixLimits
        );
    }
}
