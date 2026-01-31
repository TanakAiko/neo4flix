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
import sn.dev.user_service.services.UserService;
import sn.dev.user_service.web.dto.LoginDTO;
import sn.dev.user_service.web.dto.PublicProfileDTO;
import sn.dev.user_service.web.dto.RefreshTokenDTO;
import sn.dev.user_service.web.dto.RegistrationDTO;
import sn.dev.user_service.web.dto.TokenResponseDTO;
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

    @Override
    @Transactional
    public void registerUser(RegistrationDTO dto) {
        // Defensive: prevent duplicates in Neo4j (e.g., if Keycloak was reset but Neo4j kept data)
        if (userRepository.existsByUsername(dto.username())) {
            throw new RuntimeException("Username is already taken.");
        }
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("Email is already taken.");
        }

        // 1. Create the User Object for Keycloak
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(dto.username());
        kcUser.setEmail(dto.email());
        kcUser.setFirstName(dto.firstname());
        kcUser.setLastName(dto.lastname());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(false);

        // 2. Add the Password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.password());
        credential.setTemporary(false);
        kcUser.setCredentials(List.of(credential));

        // 3. Request Keycloak to create the user in your specific realm
        Response response = keycloak.realm("neo4flix").users().create(kcUser);

        if (response.getStatus() == 201) {
            // 4. Get the ID Keycloak generated
            String keycloakId = CreatedResponseUtil.getCreatedId(response);
            try {
                // Defensive: ensure uniqueness again just before save (handles race conditions)
                if (userRepository.existsByUsername(dto.username())) {
                    keycloak.realm("neo4flix").users().get(keycloakId).remove();
                    throw new RuntimeException("Username is already taken.");
                }
                if (userRepository.findByEmail(dto.email()).isPresent()) {
                    keycloak.realm("neo4flix").users().get(keycloakId).remove();
                    throw new RuntimeException("Email is already taken.");
                }

                // 5. Save the user into Neo4j
                User neo4jUser = new User(keycloakId, dto.username(), dto.email(), dto.firstname(), dto.lastname());
                userRepository.save(neo4jUser);
            } catch (Exception e) {
                // If Neo4j fails, delete the user from Keycloak to keep them in sync
                keycloak.realm("neo4flix").users().get(keycloakId).remove();
                throw new RuntimeException("Database error: Could not complete registration.");
            }
        } else if (response.getStatus() == 409) {
            throw new RuntimeException("Username or Email is already taken.");
        } else {
            // Handle cases like "User already exists" (Status 409)
            throw new RuntimeException("Registration failed in Keycloak. Status: " + response.getStatus());
        }
    }

    @Override
    public TokenResponseDTO login(LoginDTO loginDto) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        WebClient webClient = WebClient.create();

        try {
            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "password")
                            .with("client_id", "neo4flix-user-service")
                            .with("client_secret", clientSecret)
                            .with("username", loginDto.username())
                            .with("password", loginDto.password()))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            response -> Mono.error(new RuntimeException("Invalid username or password")))
                    .bodyToMono(TokenResponseDTO.class)
                    .block();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Login failed: " + e.getMessage());
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
                            response -> Mono.error(new RuntimeException("Refresh token is invalid or expired")))
                    .bodyToMono(TokenResponseDTO.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Could not refresh token: " + e.getMessage());
        }
    }

    @Override
    public UserProfileDTO getAuthenticatedUser() {
        // 1. Extract the JWT from the Spring Security Context
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            // 2. The "subject" (sub) in the JWT is our Keycloak ID
            String keycloakId = jwt.getSubject();

            // 3. Find the user in Neo4j using that ID
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
                    .orElseThrow(() -> new RuntimeException("User profile not found in database"));
        }

        throw new RuntimeException("Unauthenticated request");
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
                    .toBodilessEntity() // We don't need a response body, just the status
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    @Override
    public PublicProfileDTO getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

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
            return List.of(); // Return empty list if search is empty
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
        // 1. Get current logged-in user's username
        String myUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Prevent self-following
        if (myUsername.equals(targetUsername)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        // 3. Check if target user exists
        if (!userRepository.existsByUsername(targetUsername)) {
            throw new RuntimeException("User '" + targetUsername + "' not found");
        }

        // 4. Create relationship in Neo4j
        userRepository.followUser(myUsername, targetUsername);
    }

    @Override
    @Transactional
    public void unfollow(String targetUsername) {
        // 1. Get current logged-in user's username
        String myUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Prevent self-unfollowing
        if (myUsername.equals(targetUsername)) {
            throw new RuntimeException("You cannot unfollow yourself");
        }

        // 3. Check if target user exists
        if (!userRepository.existsByUsername(targetUsername)) {
            throw new RuntimeException("User '" + targetUsername + "' not found");
        }

        // 4. Delete relationship in Neo4j
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
            throw new RuntimeException("Username is required");
        }

        // Resolve Keycloak userId for this username
        UserRepresentation kcUser = keycloak.realm("neo4flix")
                .users()
                .searchByUsername(username, true)
                .stream()
                .findFirst()
                .orElse(null);

        if (kcUser == null || kcUser.getId() == null || kcUser.getId().isBlank()) {
            // Cleanup stale Neo4j data (if any) then fail.
            userRepository.findByUsername(username)
                    .ifPresent(u -> userRepository.deleteById(u.getKeycloakId()));
            throw new RuntimeException("Keycloak user not found: " + username);
        }

        String keycloakId = kcUser.getId();

        // Delete Neo4j node (detaches relationships by deleting the node entity)
        userRepository.findById(keycloakId).ifPresent(u -> userRepository.deleteById(keycloakId));

        // Delete Keycloak account
        keycloak.realm("neo4flix").users().get(keycloakId).remove();
    }

}
