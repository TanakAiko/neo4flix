package sen.dev.movie_service.services.impl;

import java.util.List;
import java.util.Optional;

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
    public void addToWatchlist(String userId, Integer tmdbId) {
        movieRepository.addToWatchlist(userId, tmdbId);
    }

    @Override
    public void removeFromWatchlist(String userId, Integer tmdbId) {
        movieRepository.removeFromWatchlist(userId, tmdbId);
    }

    @Override
    public List<MovieSummaryDTO> getWatchlist(String userId) {
        return movieRepository.findWatchlistByUserId(userId).stream()
                .map(MovieEntity::mapToSummaryDTO)
                .toList();
    }
}
