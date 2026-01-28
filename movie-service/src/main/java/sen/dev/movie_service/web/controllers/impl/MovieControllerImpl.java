package sen.dev.movie_service.web.controllers.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;

import sen.dev.movie_service.web.controllers.MovieController;
import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public class MovieControllerImpl implements MovieController {

    // --- Discovery ---

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getTrending() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getPopular() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> searchMovies(String title) {
        // TODO Auto-generated method stub
        return null;
    }

    // --- Details ---

    @Override
    public ResponseEntity<MovieDetailsDTO> getMovieDetails(Integer tmdbId) {
        // TODO Auto-generated method stub
        return null;
    }

    // --- Watchlist Actions ---

    @Override
    public ResponseEntity<Void> addToWatchlist(Integer tmdbId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Void> removeFromWatchlist(Integer tmdbId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<MovieSummaryDTO>> getWatchlist() {
        // TODO Auto-generated method stub
        return null;
    }
}
