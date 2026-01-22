package sn.dev.user_service.web.dto;

import lombok.Builder;

@Builder
public record UserProfileDTO(
        String username,
        String email,
        String firstname,
        String lastname) {
}