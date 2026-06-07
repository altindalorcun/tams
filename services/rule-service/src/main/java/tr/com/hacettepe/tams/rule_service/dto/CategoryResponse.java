package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Category;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        OffsetDateTime updatedAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getDepartment().getId(),
                c.getName(),
                c.getDescription(),
                c.getMinCredit(),
                c.getMinEcts(),
                c.getMinCourseCount(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
