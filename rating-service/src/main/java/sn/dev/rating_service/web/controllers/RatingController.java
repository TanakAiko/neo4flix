package sn.dev.rating_service.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.rating_service.web.dto.MovieReviewDTO;
import sn.dev.rating_service.web.dto.RatingRequestDTO;
import sn.dev.rating_service.web.dto.UserRatingDTO;

import jakarta.validation.Valid;
import java.util.List;

@RequestMapping("/api/ratings")
public interface RatingController {

    // Create or Update a rating
    @PostMapping
    ResponseEntity<Void> rateMovie(@Valid @RequestBody RatingRequestDTO request);

    // Delete a rating
    @DeleteMapping("/{tmdbId}")
    ResponseEntity<Void> deleteRating(@PathVariable Integer tmdbId);

    // Get all ratings for the current user
    @GetMapping
    ResponseEntity<List<UserRatingDTO>> getUserRatings();

    // Get a specific rating for a specific movie (e.g., to display stars on a movie
    // card)
    @GetMapping("/movie/{tmdbId}")
    ResponseEntity<Integer> getRating(@PathVariable Integer tmdbId);

    // Get average rating for a specific movie across all users
    @GetMapping("/movie/{tmdbId}/average")
    ResponseEntity<Double> getAverageRating(@PathVariable Integer tmdbId);

    // Get all reviews/ratings for a specific movie from all users (public)
    @GetMapping("/movie/{tmdbId}/reviews")
    ResponseEntity<List<MovieReviewDTO>> getMovieReviews(@PathVariable Integer tmdbId);
}