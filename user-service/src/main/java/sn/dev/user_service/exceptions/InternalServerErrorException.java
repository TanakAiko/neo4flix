package sn.dev.user_service.exceptions;

/**
 * Indicates an unexpected server-side failure.
 * Use this when you intentionally want to return an HTTP 500.
 */
public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
