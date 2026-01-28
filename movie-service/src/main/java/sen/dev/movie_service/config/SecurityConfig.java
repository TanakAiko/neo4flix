package sen.dev.movie_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // API endpoint constants
    private static final String API_MOVIES_TRENDING = "/api/movies/trending";
    private static final String API_MOVIES_POPULAR = "/api/movies/popular";
    private static final String API_MOVIES_SEARCH = "/api/movies/search";
    private static final String API_MOVIES_BY_ID = "/api/movies/{tmdbId}";
    private static final String API_MOVIES_WATCHLIST = "/api/movies/watchlist";
    private static final String API_MOVIES_WATCHLIST_ACTION = "/api/movies/*/watchlist";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // --- PUBLIC ACCESS (No JWT required) ---
                        // Users can browse the catalog without an account
                        .requestMatchers(HttpMethod.GET, API_MOVIES_TRENDING).permitAll()
                        .requestMatchers(HttpMethod.GET, API_MOVIES_POPULAR).permitAll()
                        .requestMatchers(HttpMethod.GET, API_MOVIES_SEARCH).permitAll()
                        .requestMatchers(HttpMethod.GET, API_MOVIES_BY_ID).permitAll()

                        // --- AUTHENTICATED ACCESS (JWT required) ---
                        // Watchlist features require a logged-in user
                        .requestMatchers(HttpMethod.GET, API_MOVIES_WATCHLIST).authenticated()
                        .requestMatchers(HttpMethod.POST, API_MOVIES_WATCHLIST_ACTION).authenticated()
                        .requestMatchers(HttpMethod.DELETE, API_MOVIES_WATCHLIST_ACTION).authenticated()

                        // --- ADMIN ONLY (Role-based) ---
                        // Future: Admin endpoints can use .hasRole("ADMIN")

                        .anyRequest().permitAll())

                // Enable JWT Resource Server with Keycloak role mapping
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Maps Keycloak JWT roles (realm_access.roles) to Spring Security
     * GrantedAuthority.
     * This allows @PreAuthorize("hasRole('ADMIN')") to work with Keycloak roles.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * Extracts roles from Keycloak JWT's realm_access.roles claim.
     * Converts them to Spring Security authorities with ROLE_ prefix.
     */
    private static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> roles = (List<String>) realmAccess.get("roles");

            if (roles == null) {
                return Collections.emptyList();
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        }
    }
}