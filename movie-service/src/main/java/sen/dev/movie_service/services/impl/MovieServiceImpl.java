package sen.dev.movie_service.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sen.dev.movie_service.data.entities.MovieEntity;
import sen.dev.movie_service.data.repositories.MovieRepository;
import sen.dev.movie_service.services.MovieService;
import sen.dev.movie_service.services.TmdbService;
import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    // --- 1. DISCOVERY METHODS (Passthrough) ---

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> getTrendingMovies() {
        try {
            return tmdbService.fetchTrendingMovies();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trending movies", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> getPopularMovies() {
        try {
            return tmdbService.fetchPopularMovies();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch popular movies", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> searchMovies(String title) {
        try {
            return tmdbService.searchMovies(title);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search movies", e);
        }
    }

    // --- 2. DETAILS (Lazy Loading) ---

    @Override
    public MovieDetailsDTO getMovieByTmdbId(Integer tmdbId) {
        // 1. Check DB first
        Optional<MovieEntity> existingMovie = movieRepository.findByTmdbId(tmdbId);

        MovieEntity movieEntity;

        if (existingMovie.isPresent()) {
            movieEntity = existingMovie.get();
        } else {
            // 2. If not found, fetch from TMDB and Save
            try {
                movieEntity = tmdbService.fetchAndMapMovieDetails(tmdbId);
                movieRepository.save(movieEntity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load movie from TMDB", e);
            }
        }

        // 3. Convert Entity to DTO
        return movieEntity.mapToDetailsDTO();
    }

    // --- 3. WATCHLIST ---

    @Override
    public void addToWatchlist(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        movieRepository.addToWatchlist(userId, tmdbId);
    }

    @Override
    public void removeFromWatchlist(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        movieRepository.removeFromWatchlist(userId, tmdbId);
    }

    @Override
    public List<MovieSummaryDTO> getWatchlist() {
        String userId = getAuthenticatedUserId();
        return movieRepository.findWatchlistByUserId(userId).stream()
                .map(MovieEntity::mapToSummaryDTO)
                .toList();
    }

    // --- Helper Methods ---

    /**
     * Extracts the authenticated user's Keycloak ID (sub claim) from SecurityContextHolder.
     * This follows the same pattern as user-service.
     */
    private String getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        throw new RuntimeException("Unauthenticated request - no valid JWT found");
    }
}
