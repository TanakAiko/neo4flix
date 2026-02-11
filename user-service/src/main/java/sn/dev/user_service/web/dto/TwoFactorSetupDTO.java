package sn.dev.user_service.web.dto;

/**
 * Returned when a user initiates 2FA setup.
 * Contains the TOTP secret and the otpauth:// URI for QR code generation.
 *
 * @param secret   The raw TOTP secret (Base32 encoded)
 * @param otpAuthUri The otpauth:// URI for scanning with authenticator apps
 * @param qrCode   Base64-encoded PNG image of the QR code (optional, for direct display)
 */
public record TwoFactorSetupDTO(
        String secret,
        String otpAuthUri
) {
}
