package sn.dev.user_service.services;

import java.util.List;

import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.PublicProfileDTO;
import sn.dev.user_service.web.dto.RefreshTokenDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
import sn.dev.user_service.web.dto.TwoFactorSetupDTO;
import sn.dev.user_service.web.dto.TwoFactorStatusDTO;
import sn.dev.user_service.web.dto.UserProfileDTO;

public interface UserService {
    void registerUser(RegistrationDTO registrationDto);

    TokenResponseDTO login(LoginDTO loginDto);

    UserProfileDTO getAuthenticatedUser();

    TokenResponseDTO refreshToken(RefreshTokenDTO refreshTokenDto);

    void logout(RefreshTokenDTO refreshTokenDto);

    PublicProfileDTO getPublicProfile(String username);

    List<PublicProfileDTO> searchUsers(String query);

    void follow(String targetUsername);

    void unfollow(String targetUsername);

    List<PublicProfileDTO> getFollowingList(String username);

    List<PublicProfileDTO> getFollowersList(String username);

    void adminDeleteUser(String username);

    // --- Two-Factor Authentication ---

    TwoFactorStatusDTO getTwoFactorStatus();

    TwoFactorSetupDTO enableTwoFactor();

    void verifyAndActivateTwoFactor(String totpCode);

    void disableTwoFactor();
}
