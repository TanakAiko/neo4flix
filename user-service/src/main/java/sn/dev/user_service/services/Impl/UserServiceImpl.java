package sn.dev.user_service.services.Impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

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

                User neo4jUser = User.builder()
                        .keycloakId(keycloakId)
                        .username(dto.username())
                        .email(dto.email())
                        .firstname(dto.firstname())
                        .lastname(dto.lastname())
                        .build();
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
            // Check if 2FA is enabled for this user via Neo4j
            userRepository.findByUsername(loginDto.username()).ifPresent(user -> {
                String storedSecret = user.getTotpSecret();
                if (storedSecret != null && !storedSecret.isBlank()) {
                    // 2FA is enabled — TOTP code is required
                    if (loginDto.totp() == null || loginDto.totp().isBlank()) {
                        throw new BadRequestException("2FA code is required or invalid");
                    }
                    if (!verifyTotpCode(storedSecret, loginDto.totp())) {
                        throw new BadRequestException("2FA code is required or invalid");
                    }
                }
            });

            // Build the form data
            var formData = BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", "neo4flix-user-service")
                    .with("client_secret", clientSecret)
                    .with("username", loginDto.username())
                    .with("password", loginDto.password());

            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            response -> response.bodyToMono(String.class).flatMap(body -> {
                                log.warn("Keycloak token error [status={}]: {}",
                                        response.statusCode().value(), body);
                                return Mono.error(
                                        new BadRequestException("Invalid username or password"));
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
    public List<PublicProfileDTO> listAllUsers(int limit) {
        return userRepository.findAllUsersLimited(limit).stream()
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
     * In-memory store for pending TOTP secrets during the enable → verify flow.
     * Key: Keycloak user ID, Value: Base32-encoded TOTP secret.
     * The secret is removed after successful verification or cancellation.
     */
    private static final Map<String, String> pendingTotpSecrets = new ConcurrentHashMap<>();

    /**
     * Returns the current 2FA status for the authenticated user.
     * Checks the totpSecret field on the User node in Neo4j.
     */
    @Override
    public TwoFactorStatusDTO getTwoFactorStatus() {
        String keycloakId = getAuthenticatedKeycloakId();
        boolean otpConfigured = userRepository.findById(keycloakId)
                .map(user -> user.getTotpSecret() != null && !user.getTotpSecret().isBlank())
                .orElse(false);
        return new TwoFactorStatusDTO(otpConfigured);
    }

    /**
     * Initiates 2FA setup: generates a TOTP secret, stores it in-memory,
     * and returns the secret + otpauth URI for the user to scan.
     * Does NOT activate 2FA yet — the user must verify with a valid code first.
     */
    @Override
    public TwoFactorSetupDTO enableTwoFactor() {
        String keycloakId = getAuthenticatedKeycloakId();

        User neo4jUser = userRepository.findById(keycloakId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        if (neo4jUser.getTotpSecret() != null && !neo4jUser.getTotpSecret().isBlank()) {
            throw new ConflictException("Two-factor authentication is already enabled");
        }

        // Generate a TOTP secret and store it in-memory
        String secret = generateTotpSecret();
        pendingTotpSecrets.put(keycloakId, secret);

        String otpAuthUri = String.format(
                "otpauth://totp/Neo4flix:%s?secret=%s&issuer=Neo4flix&digits=6&period=30&algorithm=HmacSHA1",
                neo4jUser.getUsername(), secret);

        return new TwoFactorSetupDTO(secret, otpAuthUri);
    }

    /**
     * Verifies the TOTP code against the pending secret stored in-memory.
     * If valid, persists the TOTP secret to the User node in Neo4j
     * so that subsequent logins can be validated by our service.
     */
    @Override
    @Transactional
    public void verifyAndActivateTwoFactor(String totpCode) {
        String keycloakId = getAuthenticatedKeycloakId();

        User neo4jUser = userRepository.findById(keycloakId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        if (neo4jUser.getTotpSecret() != null && !neo4jUser.getTotpSecret().isBlank()) {
            throw new ConflictException("Two-factor authentication is already enabled");
        }

        String secret = pendingTotpSecrets.get(keycloakId);
        if (secret == null) {
            throw new BadRequestException("2FA setup was not initiated. Call the enable endpoint first.");
        }

        // Validate the TOTP code against the pending secret
        if (!verifyTotpCode(secret, totpCode)) {
            throw new BadRequestException("Invalid TOTP code. Please try again.");
        }

        // Persist the TOTP secret in Neo4j
        neo4jUser.setTotpSecret(secret);
        userRepository.save(neo4jUser);

        // Remove CONFIGURE_TOTP required action in Keycloak if present so login isn't blocked
        try {
            UserResource userResource = keycloak.realm("neo4flix").users().get(keycloakId);
            UserRepresentation kcUser = userResource.toRepresentation();
            if (kcUser.getRequiredActions() != null && kcUser.getRequiredActions().contains("CONFIGURE_TOTP")) {
                kcUser.getRequiredActions().remove("CONFIGURE_TOTP");
                userResource.update(kcUser);
            }
        } catch (Exception e) {
            log.warn("Could not clear CONFIGURE_TOTP required action in Keycloak", e);
        }

        // Clean up in-memory store
        pendingTotpSecrets.remove(keycloakId);
    }

    /**
     * Disables 2FA by clearing the totpSecret field on the User node in Neo4j.
     */
    @Override
    @Transactional
    public void disableTwoFactor() {
        String keycloakId = getAuthenticatedKeycloakId();

        User neo4jUser = userRepository.findById(keycloakId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        if (neo4jUser.getTotpSecret() == null || neo4jUser.getTotpSecret().isBlank()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        // Clear the TOTP secret in Neo4j
        neo4jUser.setTotpSecret(null);
        userRepository.save(neo4jUser);

        // Clean up in-memory store in case it's lingering
        pendingTotpSecrets.remove(keycloakId);
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
     * Generates a random Base32-encoded 20-byte TOTP secret (160-bit, per RFC 4226).
     * Returns uppercase, no-padding Base32 — the standard format for authenticator apps.
     */
    private String generateTotpSecret() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32(false); // false = no line separator
        return base32.encodeAsString(bytes).replace("=", "").toUpperCase();
    }

    /**
     * Validates a TOTP code against the given secret.
     * Allows a ±1 time-step window (90 seconds total) to account for clock skew.
     */
    private boolean verifyTotpCode(String base32Secret, String code) {
        if (code == null || code.length() != 6) {
            return false;
        }
        try {
            long timeIndex = System.currentTimeMillis() / 1000 / 30;
            for (int i = -1; i <= 1; i++) {
                String candidate = generateTotpForTime(base32Secret, timeIndex + i);
                if (candidate.equals(code)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * Generates a 6-digit TOTP code for the given Base32 secret and time index.
     * Compatible with Google Authenticator (HmacSHA1, 6 digits, 30s period).
     */
    private String generateTotpForTime(String base32Secret, long timeIndex) throws Exception {
        Base32 base32 = new Base32(false);
        // Normalize: uppercase, no padding — matches what authenticator apps use
        String normalized = base32Secret.toUpperCase().replace("=", "").trim();
        byte[] key = base32.decode(normalized);

        byte[] data = new byte[8];
        long value = timeIndex;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

}
