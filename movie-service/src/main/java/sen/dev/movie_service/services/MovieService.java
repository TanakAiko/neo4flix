package sen.dev.movie_service.services;

import java.util.List;

import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public interface MovieService {

    // --- DISCOVERY (Passthrough) ---
    // These methods just pass the request to TmdbService and return DTOs directly.
    // We do NOT save these to the database.

    List<MovieSummaryDTO> getTrendingMovies();

    List<MovieSummaryDTO> getPopularMovies();

    List<MovieSummaryDTO> searchMovies(String title);

    // --- DETAILS (Lazy Loading) ---
    // This method checks the DB first.
    // If missing, it asks TmdbService to build the Entity, saves it to DB,
    // and then converts it to a DTO to return to the frontend.

    MovieDetailsDTO getMovieByTmdbId(Integer tmdbId);

    // --- WATCHLIST (Database Operations) ---
    // These methods interact directly with Neo4j to manage User relationships.
    // User ID is extracted from SecurityContextHolder (JWT sub claim).

    void addToWatchlist(Integer tmdbId);

    void removeFromWatchlist(Integer tmdbId);

    List<MovieSummaryDTO> getWatchlist();
}
