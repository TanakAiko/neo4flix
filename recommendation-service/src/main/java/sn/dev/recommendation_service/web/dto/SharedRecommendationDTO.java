package sn.dev.recommendation_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a shared recommendation received by a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedRecommendationDTO {
    
    // Movie info
    private Integer tmdbId;
    private String title;
    private String posterPath;
    private String overview;
    private Double voteAverage;
    private Integer releaseYear;
    
    // Share info
    private String fromUsername;
    private String message;
    private LocalDateTime sharedAt;
}
