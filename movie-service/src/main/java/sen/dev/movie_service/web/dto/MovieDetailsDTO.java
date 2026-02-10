package sen.dev.movie_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDetailsDTO {
    private Integer tmdbId;
    private String title;
    private String overview;
    private LocalDate releaseDate;
    private String posterPath;
    private String backdropPath;
    private Double voteAverage;

    private List<String> genres;

    // Directors and Cast now include their TMDB ID, Name, and Profile Picture
    private List<PersonDTO> directors;
    private List<PersonDTO> cast;
}