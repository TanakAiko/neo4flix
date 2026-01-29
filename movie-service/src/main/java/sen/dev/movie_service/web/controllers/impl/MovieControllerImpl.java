package sen.dev.movie_service.web.controllers.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sen.dev.movie_service.services.MovieService;
import sen.dev.movie_service.web.controllers.MovieController;
import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

@RestController
@RequiredArgsConstructor
public class MovieControllerImpl implements MovieController {

    private final MovieService movieService;

    // --- Discovery ---

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getTrending() {
        List<MovieSummaryDTO> trendingMovies = movieService.getTrendingMovies();
        return ResponseEntity.ok(trendingMovies);
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getPopular() {
        List<MovieSummaryDTO> popularMovies = movieService.getPopularMovies();
        return ResponseEntity.ok(popularMovies);
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> searchMovies(String title) {
        List<MovieSummaryDTO> searchResults = movieService.searchMovies(title);
        return ResponseEntity.ok(searchResults);
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getSimilarMovies(Integer tmdbId) {
        List<MovieSummaryDTO> similarMovies = movieService.getSimilarMovies(tmdbId);
        return ResponseEntity.ok(similarMovies);
    }

    // --- Details ---

    @Override
    public ResponseEntity<MovieDetailsDTO> getMovieDetails(Integer tmdbId) {
        MovieDetailsDTO movieDetails = movieService.getMovieByTmdbId(tmdbId);
        return ResponseEntity.ok(movieDetails);
    }

    // --- Watchlist Actions ---

    @Override
    public ResponseEntity<Void> addToWatchlist(Integer tmdbId) {
        movieService.addToWatchlist(tmdbId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> removeFromWatchlist(Integer tmdbId) {
        movieService.removeFromWatchlist(tmdbId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getWatchlist() {
        List<MovieSummaryDTO> watchlist = movieService.getWatchlist();
        return ResponseEntity.ok(watchlist);
    }
}
