package sn.dev.user_service.web.controllers.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sn.dev.user_service.services.UserService;
import sn.dev.user_service.web.controllers.UserController;
import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.PublicProfileDTO;
import sn.dev.user_service.web.dto.RefreshTokenDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
import sn.dev.user_service.web.dto.UserProfileDTO;

@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {
    private final UserService userService;

    @Override
    public ResponseEntity<String> register(RegistrationDTO registrationDto) {
        userService.registerUser(registrationDto);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TokenResponseDTO> login(LoginDTO loginDto) {
        TokenResponseDTO tokenResponse = userService.login(loginDto);
        return ResponseEntity.ok(tokenResponse);
    }

    @Override
    public ResponseEntity<TokenResponseDTO> refresh(@Valid RefreshTokenDTO refreshTokenDto) {
        TokenResponseDTO tokens = userService.refreshToken(refreshTokenDto);
        return ResponseEntity.ok(tokens);
    }

    @Override
    public ResponseEntity<UserProfileDTO> getProfile() {
        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }

    @Override
    public ResponseEntity<Void> logout(@Valid RefreshTokenDTO refreshTokenDTO) {
        userService.logout(refreshTokenDTO);
        return ResponseEntity.noContent().build(); // 204 No Content is standard for logout
    }

    @Override
    public ResponseEntity<PublicProfileDTO> getPublicProfile(String username) {
        return ResponseEntity.ok(userService.getPublicProfile(username));
    }

    @Override
    public ResponseEntity<List<PublicProfileDTO>> search(String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @Override
    public ResponseEntity<Void> follow(String username) {
        userService.follow(username);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> unfollow(String username) {
        userService.unfollow(username);
        return ResponseEntity.noContent().build();
    }
}
