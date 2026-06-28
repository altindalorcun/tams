package tr.com.hacettepe.tams.rule_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for creating a curriculum equivalence rule under a department.
 *
 * <p>Rule types:
 * <ul>
 *   <li>{@code PAIRWISE} — Each legacy[i] is individually bi-directionally equivalent to replacement[i].
 *       Lists must have equal length. Effective date is ignored.</li>
 *   <li>{@code GROUP_LEGACY_TO_REPLACEMENT} — All legacy courses passed (before effective date) →
 *       all replacement courses are considered passed.</li>
 *   <li>{@code GROUP_REPLACEMENT_TO_LEGACY} — All replacement courses passed →
 *       all legacy courses are considered passed.</li>
 *   <li>{@code GROUP_MUTUAL} — Both GROUP directions apply simultaneously.</li>
 * </ul>
 */
@Schema(description = "Defines an equivalence between courses removed from the curriculum and the new courses that replaced them")
public record CreateCurriculumEquivalenceRuleRequest(
        @Schema(description = "Rule type: PAIRWISE | GROUP_LEGACY_TO_REPLACEMENT | GROUP_REPLACEMENT_TO_LEGACY | GROUP_MUTUAL",
                example = "PAIRWISE")
        @NotBlank String ruleType,

        @Schema(description = "Courses removed from the curriculum (the 'old' set)", example = "[\"HAS222\", \"HAS223\"]")
        @NotEmpty List<String> legacyCourseCodes,

        @Schema(description = "Courses added to the curriculum (the 'new' set)", example = "[\"MUH103\", \"MUH104\"]")
        @NotEmpty List<String> replacementCourseCodes,

        @Schema(description = "Academic year the change took effect (e.g. 2019 for 2019-2020). Required for GROUP types.", example = "2019")
        Integer effectiveFromYear,

        @Schema(description = "Term within the effective year: GUZ or BAHAR. Null means start of academic year.", example = "GUZ")
        @Pattern(regexp = "GUZ|BAHAR", message = "effectiveFromTerm must be GUZ or BAHAR")
        String effectiveFromTerm
) {}
