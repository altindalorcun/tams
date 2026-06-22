package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a new department. */
@Schema(description = "Request body for creating a new department")
public record CreateDepartmentRequest(
        @Schema(description = "Unique department name", example = "Bilgisayar Mühendisliği")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Unique short department code", example = "CENG")
        @NotBlank @Size(max = 20) String code,

        @Schema(description = "Optional description", example = "Department of Computer Engineering")
        String description,

        @Schema(description = "Minimum total ECTS required to graduate; null means no threshold", example = "240")
        BigDecimal minTotalEcts,

        @Schema(description = "Whether a single F grade blocks graduation eligibility", example = "false")
        Boolean blockOnAnyFGrade
) {}
