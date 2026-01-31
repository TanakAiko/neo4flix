package sn.dev.user_service.exceptions;

/**
 * Indicates the client sent an invalid request.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
