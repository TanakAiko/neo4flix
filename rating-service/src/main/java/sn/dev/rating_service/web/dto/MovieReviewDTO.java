package sn.dev.rating_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a single review/rating on a movie by any user.
 * Used for the public "movie reviews" endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieReviewDTO {
    private String username;
    private Integer score;
    private String comment;
    private LocalDateTime ratedDate;
}
