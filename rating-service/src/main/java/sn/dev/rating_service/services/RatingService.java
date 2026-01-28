package sn.dev.rating_service.services;

import sn.dev.rating_service.web.dto.RatingRequestDTO;
import sn.dev.rating_service.web.dto.UserRatingDTO;

import java.util.List;

public interface RatingService {

    /**
     * Creates or updates a rating for a movie.
     * Throws an exception if the movie does not exist in the local graph.
     */
    void rateMovie(RatingRequestDTO request);

    void deleteRating(Integer tmdbId);

    /**
     * Fetches all ratings for the user, sorted by most recent.
     */
    List<UserRatingDTO> getUserRatings();

    /**
     * Fetches a specific rating for a movie.
     * Returns null if the user hasn't rated this movie yet.
     */
    Integer getRating(Integer tmdbId);

    Double getAverageRating(Integer tmdbId);
}