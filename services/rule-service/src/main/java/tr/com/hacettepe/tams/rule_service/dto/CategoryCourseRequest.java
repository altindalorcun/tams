package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request body for adding a course to a graduation category. */
public record CategoryCourseRequest(
        @NotNull UUID courseId,
        boolean isMandatory
) {}
