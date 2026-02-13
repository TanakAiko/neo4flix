package sn.dev.rating_service.services.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dev.rating_service.data.repositories.RatingRepository;
import sn.dev.rating_service.exceptions.NotFoundException;
import sn.dev.rating_service.exceptions.BadRequestException;
import sn.dev.rating_service.services.RatingService;
import sn.dev.rating_service.web.dto.RatingRequestDTO;
import sn.dev.rating_service.web.dto.UserRatingDTO;
import sn.dev.rating_service.web.dto.MovieReviewDTO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;

    @Override
    @Transactional
    public void rateMovie(RatingRequestDTO request) {
        String userId = getAuthenticatedUserId();

        // Call the repository Cypher query
        // It returns the tmdbId if successful, or empty if the movie node wasn't found
        ratingRepository.rateMovie(userId, request.getTmdbId(), request.getScore(), request.getComment())
            .orElseThrow(() -> new NotFoundException("Movie with ID " + request.getTmdbId() +
                    " not found. Please ensure the movie exists in the system before rating."));
    }

    @Override
    @Transactional
    public void deleteRating(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        long deleted = ratingRepository.deleteRating(userId, tmdbId);
        
        if (deleted == 0) {
            throw new NotFoundException("No rating found for movie with ID " + tmdbId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRatingDTO> getUserRatings() {
        String userId = getAuthenticatedUserId();
        List<Map<String, Object>> results = ratingRepository.findUserRatings(userId);

        return results.stream()
                .map(this::mapToUserRatingDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getRating(Integer tmdbId) {
        String userId = getAuthenticatedUserId();
        return ratingRepository.findRatingByUserAndMovie(userId, tmdbId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Integer tmdbId) {
        return ratingRepository.findAverageRatingByTmdbId(tmdbId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieReviewDTO> getMovieReviews(Integer tmdbId) {
        List<Map<String, Object>> results = ratingRepository.findAllRatingsForMovie(tmdbId);
        return results.stream()
                .map(this::mapToMovieReviewDTO)
                .toList();
    }

    // Helper to convert raw Map to MovieReviewDTO
    private MovieReviewDTO mapToMovieReviewDTO(Map<String, Object> map) {
        return MovieReviewDTO.builder()
                .username((String) map.get("username"))
                .score(toInteger(map.get("score")))
                .comment((String) map.get("comment"))
                .ratedDate(convertOffsetToLocal(map.get("ratedDate")))
                .build();
    }

    // Helper to convert raw Map from Neo4j to DTO
    private UserRatingDTO mapToUserRatingDTO(Map<String, Object> map) {
        return UserRatingDTO.builder()
                .tmdbId(toInteger(map.get("tmdbId")))
                .title((String) map.get("title"))
                .posterPath((String) map.get("posterPath"))
                .score(toInteger(map.get("score")))
                .comment((String) map.get("comment"))
                .ratedDate(convertOffsetToLocal(map.get("ratedDate")))
                .build();
    }

    // Helper to safely convert Neo4j Long to Integer
    private Integer toInteger(Object value) {
        if (value instanceof Long l) {
            return l.intValue();
        }
        if (value instanceof Integer i) {
            return i;
        }
        return null;
    }

    // Helper to handle Neo4j's OffsetDateTime to Java LocalDateTime
    private java.time.LocalDateTime convertOffsetToLocal(Object date) {
        if (date instanceof OffsetDateTime) {
            return ((OffsetDateTime) date).toLocalDateTime();
        }
        return null;
    }

    private String getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        throw new BadRequestException("Unauthenticated request - no valid JWT found");
    }
}