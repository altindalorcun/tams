package tr.com.hacettepe.tams.analysis_service.exception;

/**
 * Thrown when an authenticated user attempts to access a resource they do not own.
 * Maps to HTTP 403 via {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
