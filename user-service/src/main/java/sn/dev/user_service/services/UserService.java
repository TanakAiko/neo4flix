package sn.dev.user_service.services;

import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    /**
     * Synchronizes the user from Keycloak JWT into the Neo4j database.
     */
    void syncUser(Jwt jwt);
}
