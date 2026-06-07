package tr.com.hacettepe.tams.rule_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a course in the global catalog. */
public record CreateCourseRequest(
        @NotBlank @Size(max = 20) String courseCode,
        @NotBlank @Size(max = 255) String courseName,
        @Positive BigDecimal credit,
        @Positive BigDecimal ects
) {}
