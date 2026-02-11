package sn.dev.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        // Optional: Only required when user has 2FA enabled
        String totp
) {
}