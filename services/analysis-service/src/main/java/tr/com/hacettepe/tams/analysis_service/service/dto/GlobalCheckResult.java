package tr.com.hacettepe.tams.analysis_service.service.dto;

/**
 * Result of a single department-level global graduation check (e.g. total ECTS threshold or fail-grade block).
 *
 * @param checkType the type of global rule that was evaluated
 * @param passed    true when the student satisfies this rule
 * @param detail    human-readable explanation of why the check passed or failed
 */
public record GlobalCheckResult(
        GlobalCheckType checkType,
        boolean passed,
        String detail
) {
    /** Identifies which global graduation rule was evaluated. */
    public enum GlobalCheckType {
        /** Checks whether the student's total earned ECTS meets the department minimum. */
        TOTAL_ECTS,
        /** Checks whether the student has any failed (F-grade) courses when the department prohibits them. */
        FAIL_GRADE
    }
}
