package sn.dev.rating_service.web.controllers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.dev.rating_service.services.RatingService;
import sn.dev.rating_service.web.controllers.RatingController;
import sn.dev.rating_service.web.dto.MovieReviewDTO;
import sn.dev.rating_service.web.dto.RatingRequestDTO;
import sn.dev.rating_service.web.dto.UserRatingDTO;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RatingControllerImpl implements RatingController {

    private final RatingService ratingService;

    @Override
    public ResponseEntity<Void> rateMovie(RatingRequestDTO request) {
        ratingService.rateMovie(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteRating(Integer tmdbId) {
        ratingService.deleteRating(tmdbId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @Override
    public ResponseEntity<List<UserRatingDTO>> getUserRatings() {
        List<UserRatingDTO> ratings = ratingService.getUserRatings();
        return ResponseEntity.ok(ratings);
    }

    @Override
    public ResponseEntity<Integer> getRating(Integer tmdbId) {
        Integer rating = ratingService.getRating(tmdbId);
        // Return the score (1-5) or null if the user hasn't rated it
        return ResponseEntity.ok(rating);
    }

    @Override
    public ResponseEntity<Double> getAverageRating(Integer tmdbId) {
        Double averageRating = ratingService.getAverageRating(tmdbId);
        
        if (averageRating == null) {
            // No ratings exist for this movie yet
            return ResponseEntity.ok(0.0);
        }
        
        return ResponseEntity.ok(averageRating);
    }

    @Override
    public ResponseEntity<List<MovieReviewDTO>> getMovieReviews(Integer tmdbId) {
        List<MovieReviewDTO> reviews = ratingService.getMovieReviews(tmdbId);
        return ResponseEntity.ok(reviews);
    }
}