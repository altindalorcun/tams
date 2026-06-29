package tr.com.hacettepe.tams.rule_service.util;

import tr.com.hacettepe.tams.rule_service.exception.ConflictException;

/**
 * Validates enrollment cohort boundary fields on category-course assignments.
 * Mirrors the comparison semantics used by analysis-service {@code EnrollmentCohortComparator}.
 */
public final class EnrollmentCohortBoundaryValidator {

    private static final String TERM_GUZ = "GUZ";
    private static final String TERM_BAHAR = "BAHAR";

    private EnrollmentCohortBoundaryValidator() {
    }

    /**
     * Validates that optional from/to bounds are well-formed and non-overlapping.
     *
     * @throws ConflictException when a year is set without a valid term, or from is not before to
     */
    public static void validate(Integer appliesFromYear, String appliesFromTerm,
                                Integer appliesToYear, String appliesToTerm) {
        validateTerm(appliesFromYear, appliesFromTerm, "appliesFromTerm");
        validateTerm(appliesToYear, appliesToTerm, "appliesToTerm");

        if (appliesFromYear != null && appliesToYear != null) {
            int fromOrd = cohortOrdinal(appliesFromYear, appliesFromTerm);
            int toOrd = cohortOrdinal(appliesToYear, appliesToTerm);
            if (fromOrd >= toOrd) {
                throw new ConflictException(
                        "appliesFrom boundary must be strictly before appliesTo boundary");
            }
        }
    }

    private static void validateTerm(Integer year, String term, String fieldName) {
        if (year == null) {
            return;
        }
        if (term != null && !TERM_GUZ.equals(term) && !TERM_BAHAR.equals(term)) {
            throw new ConflictException(fieldName + " must be GUZ or BAHAR when set");
        }
    }

    private static int cohortOrdinal(int year, String term) {
        int termOrder = TERM_BAHAR.equals(term) ? 0 : 1;
        return year * 2 + termOrder;
    }
}
