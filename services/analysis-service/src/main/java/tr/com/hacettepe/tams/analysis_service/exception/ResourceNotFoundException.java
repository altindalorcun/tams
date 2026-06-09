package tr.com.hacettepe.tams.analysis_service.exception;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 via {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
