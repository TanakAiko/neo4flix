package sen.dev.movie_service.services.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.uwetrottmann.tmdb2.entities.BaseMovie;
import com.uwetrottmann.tmdb2.entities.Trending;

import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public class Utils {

    public static List<MovieSummaryDTO> mapToMovieSummaryDTOListTrending(List<Trending> trendings) {
        return trendings.stream()
                .map(Utils::mapToMovieSummaryDTOTending)
                .collect(Collectors.toList());
    }

    public static List<MovieSummaryDTO> mapToMovieSummaryDTOList(List<BaseMovie> movies) {
        return movies.stream()
                .map(Utils::mapToMovieSummaryDTO)
                .collect(Collectors.toList());
    }

    private static MovieSummaryDTO mapToMovieSummaryDTOTending(Trending tmdbMovie) {
        return mapToMovieSummaryDTO(tmdbMovie.movie);
    }

    private static MovieSummaryDTO mapToMovieSummaryDTO(BaseMovie tmdbMovie) {
        return MovieSummaryDTO.builder()
                .tmdbId(tmdbMovie.id)
                .title(tmdbMovie.title)
                .overview(tmdbMovie.overview)
                .posterPath(tmdbMovie.poster_path)
                .voteAverage(tmdbMovie.vote_average)
                .releaseYear(extractYear(tmdbMovie.release_date))
                .build();
    }

    /**
     * Extracts the year from a java.util.Date.
     * TMDB-java 2.13.0 uses Date objects for date fields.
     */
    private static Integer extractYear(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .getYear();
    }

    /**
     * Converts java.util.Date to LocalDate.
     * TMDB-java 2.13.0 uses Date objects for release_date fields.
     */
    public static LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
