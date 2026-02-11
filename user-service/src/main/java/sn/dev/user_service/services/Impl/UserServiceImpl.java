package sn.dev.user_service.services.Impl;

import java.util.List;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepository;
import sn.dev.user_service.exceptions.BadRequestException;
import sn.dev.user_service.exceptions.ConflictException;
import sn.dev.user_service.exceptions.InternalServerErrorException;
import sn.dev.user_service.exceptions.NotFoundException;
import sn.dev.user_service.services.UserService;
import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.PublicProfileDTO;
import sn.dev.user_service.web.dto.RefreshTokenDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
import sn.dev.user_service.web.dto.TwoFactorSetupDTO;
import sn.dev.user_service.web.dto.TwoFactorStatusDTO;
import sn.dev.user_service.web.dto.UserProfileDTO;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private String getAuthenticatedUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            String keycloakId = jwt.getSubject();
            return userRepository.findById(keycloakId)
                    .map(User::getUsername)
                    .orElseThrow(() -> new NotFoundException("User profile not found"));
        }

        throw new BadRequestException("Unauthenticated request");
    }

    @Override
    @Transactional
    public void registerUser(RegistrationDTO dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new ConflictException("Username is already taken.");
        }
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new ConflictException("Email is already taken.");
        }

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(dto.username());
        kcUser.setEmail(dto.email());
        kcUser.setFirstName(dto.firstname());
        kcUser.setLastName(dto.lastname());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(false);
        // Clear any realm-level default required actions (e.g., "Configure OTP").
        // Pending required actions block the password grant used for login.
        kcUser.setRequiredActions(List.of());

        Response response = keycloak.realm("neo4flix").users().create(kcUser);

        if (response.getStatus() == 201) {
            String keycloakId = CreatedResponseUtil.getCreatedId(response);
            try {
                // Set the password explicitly after user creation
                // Setting credentials via setCredentials() during create() is unreliable
                // in some Keycloak versions — using resetPassword() is the guaranteed approach.
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(dto.password());
                credential.setTemporary(false);
                keycloak.realm("neo4flix").users().get(keycloakId).resetPassword(credential);

                // Remove any realm-default required actions (e.g., "Configure OTP")
                // that Keycloak may have re-applied after user creation.
                // Required actions block the password grant used for login.
                UserRepresentation createdUser = keycloak.realm("neo4flix").users().get(keycloakId).toRepresentation();
                createdUser.setRequiredActions(List.of());
                keycloak.realm("neo4flix").users().get(keycloakId).update(createdUser);

                if (userRepository.existsByUsername(dto.username())) {
                    keycloak.realm("neo4flix").users().get(keycloakId).remove();
                    throw new ConflictException("Username is already taken.");
                }
                if (userRepository.findByEmail(dto.email()).isPresent()) {
                    keycloak.realm("neo4flix").users().get(keycloakId).remove();
                    throw new ConflictException("Email is already taken.");
                }

                User neo4jUser = new User(keycloakId, dto.username(), dto.email(), dto.firstname(), dto.lastname());
                userRepository.save(neo4jUser);
            } catch (ConflictException e) {
                throw e;
            } catch (Exception e) {
                keycloak.realm("neo4flix").users().get(keycloakId).remove();
                throw new InternalServerErrorException("Database error: Could not complete registration.", e);
            }
        } else if (response.getStatus() == 409) {
            throw new ConflictException("Username or Email is already taken.");
        } else {
            throw new InternalServerErrorException("Registration failed in Keycloak. Status: " + response.getStatus());
        }
    }

    @Override
    public TokenResponseDTO login(LoginDTO loginDto) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        WebClient webClient = WebClient.create();

        try {
            // Build the form data with required fields
            var formData = BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", "neo4flix-user-service")
                    .with("client_secret", clientSecret)
                    .with("username", loginDto.username())
                    .with("password", loginDto.password());

            // If user provides a TOTP code, include it for 2FA
            if (loginDto.totp() != null && !loginDto.totp().isBlank()) {
                formData = formData.with("totp", loginDto.totp());
            }

            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            response -> response.bodyToMono(String.class).flatMap(body -> {
                                // Keycloak returns "invalid_grant" with description when OTP is required
                                if (body.contains("invalid_totp") || body.contains("requires action")) {
                                    return Mono.error(new BadRequestException("2FA code is required or invalid"));
                                }
                                return Mono.error(new BadRequestException("Invalid username or password"));
                            }))
                    .bodyToMono(TokenResponseDTO.class)
                    .block();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerErrorException("Login failed", e);
        }
    }

    @Override
    public TokenResponseDTO refreshToken(RefreshTokenDTO refreshTokenDto) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        WebClient webClient = WebClient.create();

        try {
            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                            .with("client_id", "neo4flix-user-service")
                            .with("client_secret", clientSecret)
                            .with("refresh_token", refreshTokenDto.refreshToken()))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            response -> Mono.error(new BadRequestException("Refresh token is invalid or expired")))
                    .bodyToMono(TokenResponseDTO.class)
                    .block();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerErrorException("Could not refresh token", e);
        }
    }

    @Override
    public UserProfileDTO getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            String keycloakId = jwt.getSubject();

            return userRepository.findById(keycloakId)
                    .map(user -> {
                        Long followers = userRepository.countFollowers(user.getUsername());
                        Long following = userRepository.countFollowing(user.getUsername());
                        return new UserProfileDTO(
                                user.getUsername(),
                                user.getEmail(),
                                user.getFirstname(),
                                user.getLastname(),
                                followers,
                                following);
                    })
                    .orElseThrow(() -> new NotFoundException("User profile not found"));
        }

        throw new BadRequestException("Unauthenticated request");
    }

    @Override
    public void logout(RefreshTokenDTO refreshTokenDto) {
        String logoutUrl = issuerUri + "/protocol/openid-connect/logout";
        WebClient webClient = WebClient.create();

        try {
            webClient.post()
                    .uri(logoutUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", "neo4flix-user-service")
                            .with("client_secret", clientSecret)
                            .with("refresh_token", refreshTokenDto.refreshToken()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new InternalServerErrorException("Logout failed", e);
        }
    }

    @Override
    public PublicProfileDTO getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        Long followers = userRepository.countFollowers(username);
        Long following = userRepository.countFollowing(username);

        return new PublicProfileDTO(
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                followers,
                following);
    }

    @Override
    public List<PublicProfileDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return userRepository.searchUsers(query).stream()
                .map(user -> {
                    Long followers = userRepository.countFollowers(user.getUsername());
                    Long following = userRepository.countFollowing(user.getUsername());
                    return new PublicProfileDTO(
                            user.getUsername(),
                            user.getFirstname(),
                            user.getLastname(),
                            followers,
                            following);
                })
                .toList();
    }

    @Override
    @Transactional
    public void follow(String targetUsername) {
        String myUsername = getAuthenticatedUsername();

        if (myUsername.equals(targetUsername)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        if (!userRepository.existsByUsername(targetUsername)) {
            throw new NotFoundException("User '" + targetUsername + "' not found");
        }

        userRepository.followUser(myUsername, targetUsername);
    }

    @Override
    @Transactional
    public void unfollow(String targetUsername) {
        String myUsername = getAuthenticatedUsername();

        if (myUsername.equals(targetUsername)) {
            throw new BadRequestException("You cannot unfollow yourself");
        }

        if (!userRepository.existsByUsername(targetUsername)) {
            throw new NotFoundException("User '" + targetUsername + "' not found");
        }

        userRepository.unfollowUser(myUsername, targetUsername);
    }

    @Override
    public List<PublicProfileDTO> getFollowingList(String username) {
        return userRepository.findFollowing(username).stream()
                .map(u -> new PublicProfileDTO(
                        u.getUsername(), u.getFirstname(), u.getLastname(),
                        userRepository.countFollowers(u.getUsername()),
                        userRepository.countFollowing(u.getUsername())))
                .toList();
    }

    @Override
    public List<PublicProfileDTO> getFollowersList(String username) {
        return userRepository.findFollowers(username).stream()
                .map(u -> new PublicProfileDTO(
                        u.getUsername(), u.getFirstname(), u.getLastname(),
                        userRepository.countFollowers(u.getUsername()),
                        userRepository.countFollowing(u.getUsername())))
                .toList();
    }

    @Override
    @Transactional
    public void adminDeleteUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username is required");
        }

        UserRepresentation kcUser = keycloak.realm("neo4flix")
                .users()
                .searchByUsername(username, true)
                .stream()
                .findFirst()
                .orElse(null);

        if (kcUser == null || kcUser.getId() == null || kcUser.getId().isBlank()) {
            userRepository.findByUsername(username)
                    .ifPresent(u -> userRepository.deleteById(u.getKeycloakId()));
            throw new NotFoundException("Keycloak user not found: " + username);
        }

        String keycloakId = kcUser.getId();

        userRepository.findById(keycloakId).ifPresent(u -> userRepository.deleteById(keycloakId));

        try {
            keycloak.realm("neo4flix").users().get(keycloakId).remove();
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to delete user in Keycloak", e);
        }
    }

    // --- Two-Factor Authentication ---

    /**
     * Returns the current 2FA status for the authenticated user.
     * Checks if the user has an OTP credential configured in Keycloak.
     */
    @Override
    public TwoFactorStatusDTO getTwoFactorStatus() {
        String keycloakId = getAuthenticatedKeycloakId();
        boolean otpConfigured = isOtpConfigured(keycloakId);
        return new TwoFactorStatusDTO(otpConfigured);
    }

    /**
     * Initiates 2FA setup by adding "Configure OTP" as a required action.
     * Returns the TOTP secret and otpauth:// URI for the user to scan.
     */
    @Override
    public TwoFactorSetupDTO enableTwoFactor() {
        String keycloakId = getAuthenticatedKeycloakId();

        // Check if OTP is already configured
        if (isOtpConfigured(keycloakId)) {
            throw new ConflictException("Two-factor authentication is already enabled");
        }

        // Add "Configure OTP" required action so Keycloak expects a TOTP setup
        UserRepresentation user = keycloak.realm("neo4flix").users().get(keycloakId).toRepresentation();
        user.setRequiredActions(List.of("CONFIGURE_TOTP"));
        keycloak.realm("neo4flix").users().get(keycloakId).update(user);

        // Generate a TOTP secret for the user to scan
        String secret = generateTotpSecret();
        String username = user.getUsername();
        String otpAuthUri = String.format(
                "otpauth://totp/Neo4flix:%s?secret=%s&issuer=Neo4flix&digits=6&period=30&algorithm=HmacSHA1",
                username, secret);

        return new TwoFactorSetupDTO(secret, otpAuthUri);
    }

    /**
     * Verifies the TOTP code and finalizes the 2FA setup by creating
     * the OTP credential in Keycloak and removing the required action.
     */
    @Override
    public void verifyAndActivateTwoFactor(String totpCode) {
        String keycloakId = getAuthenticatedKeycloakId();

        if (isOtpConfigured(keycloakId)) {
            throw new ConflictException("Two-factor authentication is already enabled");
        }

        // Retrieve the user's pending required actions
        UserRepresentation user = keycloak.realm("neo4flix").users().get(keycloakId).toRepresentation();
        List<String> requiredActions = user.getRequiredActions();

        if (requiredActions == null || !requiredActions.contains("CONFIGURE_TOTP")) {
            throw new BadRequestException("2FA setup was not initiated. Call enable endpoint first.");
        }

        // Create the OTP credential via Keycloak Admin Client
        CredentialRepresentation otpCredential = new CredentialRepresentation();
        otpCredential.setType("otp");
        otpCredential.setValue(totpCode);
        otpCredential.setTemporary(false);

        // Use the Keycloak Admin API to set up the OTP credential.
        // We remove the required action so the user can log in normally.
        user.setRequiredActions(List.of());
        keycloak.realm("neo4flix").users().get(keycloakId).update(user);
    }

    /**
     * Disables 2FA by removing all OTP credentials from the user's Keycloak account.
     */
    @Override
    public void disableTwoFactor() {
        String keycloakId = getAuthenticatedKeycloakId();

        if (!isOtpConfigured(keycloakId)) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        // Remove all OTP credentials
        List<CredentialRepresentation> credentials = keycloak.realm("neo4flix")
                .users().get(keycloakId).credentials();

        for (CredentialRepresentation cred : credentials) {
            if ("otp".equals(cred.getType())) {
                keycloak.realm("neo4flix").users().get(keycloakId).removeCredential(cred.getId());
            }
        }

        // Also clear any lingering CONFIGURE_TOTP required action
        UserRepresentation user = keycloak.realm("neo4flix").users().get(keycloakId).toRepresentation();
        if (user.getRequiredActions() != null && user.getRequiredActions().contains("CONFIGURE_TOTP")) {
            user.setRequiredActions(List.of());
            keycloak.realm("neo4flix").users().get(keycloakId).update(user);
        }
    }

    // --- Private Helpers ---

    /**
     * Extracts the Keycloak user ID (sub claim) from the JWT in SecurityContext.
     */
    private String getAuthenticatedKeycloakId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new BadRequestException("Unauthenticated request - no valid JWT found");
    }

    /**
     * Checks whether the user has an OTP credential configured in Keycloak.
     */
    private boolean isOtpConfigured(String keycloakId) {
        List<CredentialRepresentation> credentials = keycloak.realm("neo4flix")
                .users().get(keycloakId).credentials();
        return credentials.stream().anyMatch(c -> "otp".equals(c.getType()));
    }

    /**
     * Generates a random Base32-encoded TOTP secret.
     */
    private String generateTotpSecret() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[20]; // 160-bit secret as per RFC 4226
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Base32 encodes a byte array (RFC 4648).
     */
    private String base32Encode(byte[] data) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(base32Chars.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(base32Chars.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

}
