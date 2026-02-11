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
                            response -> Mono.error(new BadRequestException("Invalid username or password")))
                    .bodyToMono(TokenResponseDTO.class)
                    .block();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
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

        System.out.println("------------------>  User " + myUsername + " is trying to follow " + targetUsername);
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

}
