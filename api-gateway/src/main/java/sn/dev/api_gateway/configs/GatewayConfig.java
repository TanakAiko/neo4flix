package sn.dev.api_gateway.configs;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

/**
 * Gateway route configuration using Spring Cloud Gateway Server WebMVC.
 * Routes are defined programmatically using RouterFunction.
 */
@Configuration
public class GatewayConfig {

    @Value("${gateway.services.user-service}")
    private String userServiceUri;

    @Value("${gateway.services.movie-service}")
    private String movieServiceUri;

    @Value("${gateway.services.rating-service}")
    private String ratingServiceUri;

    @Value("${gateway.services.recommendation-service}")
    private String recommendationServiceUri;

    /**
     * Routes for User Service
     */
    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route("user-service")
                .route(RequestPredicates.path("/api/users/**"), HandlerFunctions.http())
                .before(BeforeFilterFunctions.routeId("user-service"))
                .before(BeforeFilterFunctions.uri(URI.create(userServiceUri)))
                .build();
    }

    /**
     * Routes for Movie Service
     */
    @Bean
    public RouterFunction<ServerResponse> movieServiceRoute() {
        return route("movie-service")
                .route(RequestPredicates.path("/api/movies/**"), HandlerFunctions.http())
                .before(BeforeFilterFunctions.routeId("movie-service"))
                .before(BeforeFilterFunctions.uri(URI.create(movieServiceUri)))
                .build();
    }

    /**
     * Routes for Rating Service
     */
    @Bean
    public RouterFunction<ServerResponse> ratingServiceRoute() {
        return route("rating-service")
                .route(RequestPredicates.path("/api/ratings/**"), HandlerFunctions.http())
                .before(BeforeFilterFunctions.routeId("rating-service"))
                .before(BeforeFilterFunctions.uri(URI.create(ratingServiceUri)))
                .build();
    }

    /**
     * Routes for Recommendation Service
     */
    @Bean
    public RouterFunction<ServerResponse> recommendationServiceRoute() {
        return route("recommendation-service")
                .route(RequestPredicates.path("/api/recommendations/**"), HandlerFunctions.http())
                .before(BeforeFilterFunctions.routeId("recommendation-service"))
                .before(BeforeFilterFunctions.uri(URI.create(recommendationServiceUri)))
                .build();
    }
}
