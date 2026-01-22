package sn.dev.user_service.services;

import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
import sn.dev.user_service.web.dto.UserProfileDTO;

public interface UserService {
    void registerUser(RegistrationDTO registrationDto);

    TokenResponseDTO login(LoginDTO loginDto);

    UserProfileDTO getAuthenticatedUser();
}
