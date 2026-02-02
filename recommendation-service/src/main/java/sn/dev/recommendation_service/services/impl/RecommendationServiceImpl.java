package sn.dev.recommendation_service.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import sn.dev.recommendation_service.data.repositories.RecommendationRepository;
import sn.dev.recommendation_service.services.RecommendationService;
import sn.dev.recommendation_service.web.dto.MovieSummaryDTO;
import sn.dev.recommendation_service.web.dto.RecommendationDTO;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final WebClient webClient;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getRecommendations(String userId) {

        List<RecommendationDTO> allRecommendations = new ArrayList<>();

        // --- STRATEGY 1: COLLABORATIVE FILTERING (Neo4j) ---
        List<Map<String, Object>> cfResults = recommendationRepository.findCollaborativeFiltering(userId);

        for (Map<String, Object> movie : cfResults) {
            allRecommendations.add(RecommendationDTO.builder()
                    .tmdbId(toInteger(movie.get("tmdbId")))
                    .title((String) movie.get("title"))
                    .overview((String) movie.get("overview"))
                    .voteAverage((Double) movie.get("voteAverage"))
                    .posterPath((String) movie.get("posterPath"))
                    .releaseYear(toInteger(movie.get("releaseYear")))
                    .reason("Popular with similar users")
                    .build());
        }

        // --- STRATEGY 2: FALLBACK TO MOVIE SERVICE ---
        // If Neo4j returns fewer than 10 results, or 0 (Cold Start)
        if (allRecommendations.size() < 10) {

            // 1. Find a seed movie (The user's favorite)
            Map<String, Object> favorite = recommendationRepository.findFavoriteMovie(userId);

            if (favorite != null) {
                Integer favoriteTmdbId = toInteger(favorite.get("tmdbId"));

                // 2. Call Movie Service via WebClient
                List<MovieSummaryDTO> similarFromMovieService = fetchSimilarFromMovieService(favoriteTmdbId);

                // 3. Merge and Deduplicate
                for (MovieSummaryDTO dto : similarFromMovieService) {
                    if (allRecommendations.stream().noneMatch(r -> r.getTmdbId().equals(dto.getTmdbId()))) {
                        allRecommendations.add(RecommendationDTO.builder()
                                .tmdbId(dto.getTmdbId())
                                .title(dto.getTitle())
                                .overview(dto.getOverview())
                                .voteAverage(dto.getVoteAverage())
                                .posterPath(dto.getPosterPath())
                                .releaseYear(dto.getReleaseYear())
                                .reason("Similar to your favorite movie")
                                .build());
                    }
                }
            }
        }
        return allRecommendations;

    }

    private List<MovieSummaryDTO> fetchSimilarFromMovieService(Integer tmdbId) {
        // Call the endpoint we created in Movie Service: GET
        // /api/movies/{tmdbId}/similar
        return webClient.get()
                .uri("/api/movies/{tmdbId}/similar", tmdbId)
                .retrieve()
                .bodyToFlux(MovieSummaryDTO.class)
                .collectList()
                .onErrorResume(e -> {
                    // If Movie Service is down, just return empty list so our app doesn't crash
                    return Mono.just(List.of());
                })
                .block();
    }

    // Helper to safely convert Neo4j Long to Integer
    private Integer toInteger(Object value) {
        if (value instanceof Long l) {
            return l.intValue();
        }
        if (value instanceof Integer i) {
            return i;
        }
        return null;
    }
}
