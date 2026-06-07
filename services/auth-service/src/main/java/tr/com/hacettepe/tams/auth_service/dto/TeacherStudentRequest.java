package tr.com.hacettepe.tams.auth_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TeacherStudentRequest(
        @NotNull UUID teacherId,
        @NotNull UUID studentId
) {}
