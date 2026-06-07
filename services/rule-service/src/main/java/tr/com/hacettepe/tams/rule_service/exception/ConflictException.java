package tr.com.hacettepe.tams.rule_service.exception;

/**
 * Thrown when an operation is logically impossible in the current state.
 * Examples: adding a course to a category that does not belong to the
 * course's department pool.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
