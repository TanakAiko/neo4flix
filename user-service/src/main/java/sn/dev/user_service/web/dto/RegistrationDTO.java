package sn.dev.user_service.web.dto;

public record RegistrationDTO(
        String username,
        String email,
        String firstname,
        String lastname,
        String password) {
}