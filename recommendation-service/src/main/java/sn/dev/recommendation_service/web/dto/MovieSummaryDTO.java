package sn.dev.recommendation_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieSummaryDTO {
    private Integer tmdbId;
    private String title;
    private String overview;
    private String posterPath;
    private Double voteAverage;
    private Integer releaseYear;
}
