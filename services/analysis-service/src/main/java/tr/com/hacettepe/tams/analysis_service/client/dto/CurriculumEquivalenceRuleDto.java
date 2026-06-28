package tr.com.hacettepe.tams.analysis_service.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Mirror of the rule-service {@code CurriculumEquivalenceRuleDto}.
 * Carries the equivalence definition that the graduation engine uses to expand
 * the set of courses a student is considered to have passed.
 *
 * <p>Rule types:
 * <ul>
 *   <li>{@code PAIRWISE} — legacy[i] and replacement[i] are bi-directionally equivalent.</li>
 *   <li>{@code GROUP_LEGACY_TO_REPLACEMENT} — all legacy courses passed → all replacement courses implied.</li>
 *   <li>{@code GROUP_REPLACEMENT_TO_LEGACY} — all replacement courses passed → all legacy courses implied.</li>
 *   <li>{@code GROUP_MUTUAL} — both GROUP directions apply.</li>
 * </ul>
 */
public record CurriculumEquivalenceRuleDto(
        UUID id,
        String ruleType,
        List<String> legacyCourseCodes,
        List<String> replacementCourseCodes,
        Integer effectiveFromYear,
        String effectiveFromTerm
) {}
