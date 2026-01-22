package sn.dev.user_service.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // API endpoint constants
    private static final String API_USERS = "/api/users";
    private static final String API_USERS_CUSTOM = "/api/users/custom";
    private static final String API_USERS_USERNAME = "/api/users/{username}";
    private static final String API_USERS_USER_USERNAME_CUSTOM = "/api/users/{username}/custom";
    private static final String API_PRODUCTS = "/api/products";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                API_USERS,
                                API_USERS_USERNAME,
                                API_USERS_CUSTOM,
                                API_USERS_USER_USERNAME_CUSTOM)
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                API_PRODUCTS)
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.PUT,
                                API_USERS_USERNAME)
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.PATCH,
                                API_USERS_USERNAME)
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                API_USERS_USERNAME)
                        .authenticated()
                        .anyRequest().permitAll() // All endpoints are publicly accessible
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // Enable JWT validation
                );
        return http.build();
    }
}