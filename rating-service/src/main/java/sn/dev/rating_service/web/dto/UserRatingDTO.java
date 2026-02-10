package sn.dev.rating_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRatingDTO {

    // Movie Details (to display what they rated)
    private Integer tmdbId;
    private String title;
    private String posterPath;

    // The Rating
    private Integer score;
    private String comment;

    // When they rated it
    private LocalDateTime ratedDate;
}