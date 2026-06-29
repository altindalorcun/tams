package tr.com.hacettepe.tams.analysis_service.service;

import org.springframework.stereotype.Component;

/**
 * Compares student enrollment cohorts (year + term) for category-course applicability bounds.
 *
 * <p>Term ordering within the same calendar year: {@code BAHAR} (spring intake) precedes
 * {@code GUZ} (fall intake). A null term defaults to {@code GUZ}.
 *
 * <p>Applicability semantics:
 * <ul>
 *   <li>{@code appliesFrom} — inclusive lower bound; null means no lower bound.</li>
 *   <li>{@code appliesTo} — exclusive upper bound; null means no upper bound.</li>
 * </ul>
 */
@Component
public class EnrollmentCohortComparator {

    private static final String TERM_GUZ = "GUZ";
    private static final String TERM_BAHAR = "BAHAR";

    /**
     * Returns true when the student's enrollment cohort falls within the assignment's
     * applicability range. When {@code enrollmentYear} is null, bounds are ignored and
     * the assignment is treated as applicable to all cohorts.
     */
    public boolean isApplicable(Integer enrollmentYear, String enrollmentTerm,
                                Integer appliesFromYear, String appliesFromTerm,
                                Integer appliesToYear, String appliesToTerm) {
        if (enrollmentYear == null) {
            return true;
        }
        if (appliesFromYear != null && compare(enrollmentYear, enrollmentTerm, appliesFromYear, appliesFromTerm) < 0) {
            return false;
        }
        if (appliesToYear != null && compare(enrollmentYear, enrollmentTerm, appliesToYear, appliesToTerm) >= 0) {
            return false;
        }
        return true;
    }

    /**
     * Compares two enrollment cohorts. Returns negative when {@code a} is before {@code b},
     * zero when equal, positive when {@code a} is after {@code b}.
     */
    public int compare(int yearA, String termA, int yearB, String termB) {
        int ordA = cohortOrdinal(yearA, termA);
        int ordB = cohortOrdinal(yearB, termB);
        return Integer.compare(ordA, ordB);
    }

    private int cohortOrdinal(int year, String term) {
        int termOrder = TERM_BAHAR.equals(normalizeTerm(term)) ? 0 : 1;
        return year * 2 + termOrder;
    }

    private String normalizeTerm(String term) {
        if (term == null || term.isBlank()) {
            return TERM_GUZ;
        }
        return term.toUpperCase();
    }
}
