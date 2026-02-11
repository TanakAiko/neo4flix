package sn.dev.user_service.web.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.PublicProfileDTO;
import sn.dev.user_service.web.dto.RefreshTokenDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
import sn.dev.user_service.web.dto.TwoFactorSetupDTO;
import sn.dev.user_service.web.dto.TwoFactorStatusDTO;
import sn.dev.user_service.web.dto.TwoFactorVerifyDTO;
import sn.dev.user_service.web.dto.UserProfileDTO;

@RequestMapping("/api/users")
public interface UserController {

    @PostMapping("/register")
    ResponseEntity<String> register(@Valid @RequestBody RegistrationDTO registrationDto);

    @PostMapping("/login")
    ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginDTO loginDto);

    @PostMapping("/refresh")
    ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshTokenDTO refreshTokenDto);

    @GetMapping("/me")
    ResponseEntity<UserProfileDTO> getProfile();

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenDTO refreshTokenDto);

    @GetMapping("/{username}")
    ResponseEntity<PublicProfileDTO> getPublicProfile(@PathVariable String username);

    @GetMapping("/search")
    ResponseEntity<List<PublicProfileDTO>> search(@RequestParam("q") String query);

    @PostMapping("/follow/{username}")
    ResponseEntity<Void> follow(@PathVariable String username);

    @DeleteMapping("/unfollow/{username}")
    ResponseEntity<Void> unfollow(@PathVariable String username);

    @GetMapping("/{username}/following")
    ResponseEntity<List<PublicProfileDTO>> getFollowing(@PathVariable String username);

    @GetMapping("/{username}/followers")
    ResponseEntity<List<PublicProfileDTO>> getFollowers(@PathVariable String username);

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{username}")
    ResponseEntity<Void> adminDeleteUser(@PathVariable String username);

    // --- Two-Factor Authentication ---

    @GetMapping("/2fa/status")
    ResponseEntity<TwoFactorStatusDTO> getTwoFactorStatus();

    @PostMapping("/2fa/enable")
    ResponseEntity<TwoFactorSetupDTO> enableTwoFactor();

    @PostMapping("/2fa/verify")
    ResponseEntity<Void> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyDTO verifyDto);

    @PostMapping("/2fa/disable")
    ResponseEntity<Void> disableTwoFactor();
}