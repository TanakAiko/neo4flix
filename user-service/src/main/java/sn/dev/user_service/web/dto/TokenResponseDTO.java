package sn.dev.user_service.web.dto;

public record TokenResponseDTO(String accessToken, String refreshToken) {
}