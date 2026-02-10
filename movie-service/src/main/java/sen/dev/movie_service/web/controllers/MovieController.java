package sen.dev.movie_service.web.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

@RequestMapping("/api/movies")
public interface MovieController {

    // --- Discovery ---

    @GetMapping("/trending")
    ResponseEntity<List<MovieSummaryDTO>> getTrending();

    @GetMapping("/popular")
    ResponseEntity<List<MovieSummaryDTO>> getPopular();

    @GetMapping("/random")
    ResponseEntity<List<MovieSummaryDTO>> getRandomMovies(@RequestParam(defaultValue = "10") int count);

    @GetMapping("/search")
    ResponseEntity<List<MovieSummaryDTO>> searchMovies(@RequestParam String title);

    @GetMapping("/{tmdbId}/similar")
    ResponseEntity<List<MovieSummaryDTO>> getSimilarMovies(@PathVariable Integer tmdbId);

    // --- Details ---

    @GetMapping("/{tmdbId}")
    ResponseEntity<MovieDetailsDTO> getMovieDetails(@PathVariable Integer tmdbId);

    // --- Watchlist Actions (Requires Authentication) ---

    @PostMapping("/{tmdbId}/watchlist")
    ResponseEntity<Void> addToWatchlist(@PathVariable Integer tmdbId);

    @DeleteMapping("/{tmdbId}/watchlist")
    ResponseEntity<Void> removeFromWatchlist(@PathVariable Integer tmdbId);

    @GetMapping("/watchlist")
    ResponseEntity<List<MovieSummaryDTO>> getWatchlist();
}
