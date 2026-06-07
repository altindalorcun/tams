package tr.com.hacettepe.tams.rule_service.dto;

import tr.com.hacettepe.tams.rule_service.domain.Department;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response body representing a department. */
public record DepartmentResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
