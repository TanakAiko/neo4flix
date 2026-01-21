package sn.dev.user_service.web.controllers.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.dev.user_service.services.UserService;
import sn.dev.user_service.web.controllers.UserController;
import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;

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
}
