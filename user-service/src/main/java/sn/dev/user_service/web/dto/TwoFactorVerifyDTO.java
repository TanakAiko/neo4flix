package sn.dev.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for verifying a TOTP code to finalize 2FA setup.
 */
public record TwoFactorVerifyDTO(
        @NotBlank(message = "TOTP code is required")
        @Size(min = 6, max = 6, message = "TOTP code must be 6 digits")
        String code
) {
}
