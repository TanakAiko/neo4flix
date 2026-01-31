package sn.dev.user_service.exceptions;

/**
 * Indicates a request conflict with current server state (HTTP 409).
 * Example: unique constraints like username/email already exists.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
