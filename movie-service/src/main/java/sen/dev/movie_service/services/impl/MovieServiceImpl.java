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
import sen.dev.movie_service.exceptions.BadRequestException;
import sen.dev.movie_service.exceptions.ConflictException;
import sen.dev.movie_service.exceptions.InternalServerErrorException;
import sen.dev.movie_service.exceptions.NotFoundException;
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
            throw new InternalServerErrorException("Failed to fetch trending movies: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> getPopularMovies() {
        try {
            return tmdbService.fetchPopularMovies();
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to fetch popular movies: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> searchMovies(String title) {
        try {
            return tmdbService.searchMovies(title);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to search movies: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> getSimilarMovies(Integer tmdbId) {
        try {
            return tmdbService.fetchSimilarMovies(tmdbId);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to fetch similar movies: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryDTO> getRandomMovies(int count) {
        try {
            return tmdbService.fetchRandomMovies(count);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to fetch random movies: " + e.getMessage());
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
                throw new NotFoundException("Movie with tmdbId " + tmdbId + " not found");
            }
        }
        
        // Check for missing runtime or lazy-loaded data issues and update if necessary
        if(existingMovie.isPresent() && (movieEntity.getRuntime() == null || movieEntity.getRuntime() == 0)) {
             try {
                 MovieEntity freshEntity = tmdbService.fetchAndMapMovieDetails(tmdbId);
                 movieEntity.setRuntime(freshEntity.getRuntime());
                 movieRepository.save(movieEntity);
             } catch (Exception e) {
                 // Log warning but don't fail, maybe just a partial update issue
             }
        }

        // 3. Convert Entity to DTO
        return movieEntity.mapToDetailsDTO();
    }

    // --- 3. WATCHLIST ---

    @Override
    public void addToWatchlist(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        
        // Validate movie exists (this will fetch from TMDB if not in DB)
        ensureMovieExists(tmdbId);
        
        // Check if already in watchlist
        if (movieRepository.isInWatchlist(userId, tmdbId)) {
            throw new ConflictException("Movie with tmdbId " + tmdbId + " is already in your watchlist");
        }
        
        movieRepository.addToWatchlist(userId, tmdbId);
    }

    @Override
    public void removeFromWatchlist(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        
        // Check if movie is in watchlist before attempting removal
        if (!movieRepository.isInWatchlist(userId, tmdbId)) {
            throw new NotFoundException("Movie with tmdbId " + tmdbId + " is not in your watchlist");
        }
        
        int deletedCount = movieRepository.removeFromWatchlist(userId, tmdbId);
        if (deletedCount == 0) {
            throw new InternalServerErrorException("Failed to remove movie from watchlist");
        }
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
     * Ensures movie exists in DB. If not, fetches from TMDB and saves it.
     */
    private void ensureMovieExists(Integer tmdbId) {
        if (movieRepository.findByTmdbId(tmdbId).isEmpty()) {
            try {
                MovieEntity movieEntity = tmdbService.fetchAndMapMovieDetails(tmdbId);
                movieRepository.save(movieEntity);
            } catch (Exception e) {
                throw new NotFoundException("Movie with tmdbId " + tmdbId + " not found in TMDB");
            }
        }
    }

    /**
     * Extracts the authenticated user's Keycloak ID (sub claim) from
     * SecurityContextHolder.
     * This follows the same pattern as user-service.
     */
    private String getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        throw new BadRequestException("Unauthenticated request - no valid JWT found");
    }
}
