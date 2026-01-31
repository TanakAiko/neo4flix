package sn.dev.user_service.exceptions;

/**
 * Indicates a requested resource was not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
