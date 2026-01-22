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
                    .map(user -> new UserProfileDTO(
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstname(),
                            user.getLastname()))
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
        return userRepository.findByUsername(username)
                .map(user -> new PublicProfileDTO(
                        user.getUsername(),
                        user.getFirstname(),
                        user.getLastname()))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
