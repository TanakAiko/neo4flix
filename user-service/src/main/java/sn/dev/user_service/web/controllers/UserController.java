package sn.dev.user_service.web.controllers;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;

@RequestMapping("/api/users")
public interface UserController {

    @PostMapping("/register")
    ResponseEntity<String> register(@RequestBody RegistrationDTO registrationDto);

    @PostMapping("/login")
    ResponseEntity<TokenResponseDTO> login(@RequestBody LoginDTO loginDto);
}