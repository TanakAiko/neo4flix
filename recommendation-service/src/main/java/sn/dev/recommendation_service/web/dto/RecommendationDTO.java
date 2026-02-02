package sn.dev.recommendation_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private Integer tmdbId;
    private String title;
    private String posterPath;
    private String overview;
    private Double voteAverage;
    private Integer releaseYear;
    private String reason; // e.g., "Popular among users who liked Inception", "Similar to The Matrix"
}
