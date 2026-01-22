package sn.dev.user_service.web.dto;

public record PublicProfileDTO(
        String username,
        String firstname,
        String lastname,
        Long followersCount,
        Long followingCount) {
}