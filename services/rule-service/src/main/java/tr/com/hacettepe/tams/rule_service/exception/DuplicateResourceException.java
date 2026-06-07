package tr.com.hacettepe.tams.rule_service.exception;

/** Thrown when a create/update operation would violate a uniqueness constraint. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
