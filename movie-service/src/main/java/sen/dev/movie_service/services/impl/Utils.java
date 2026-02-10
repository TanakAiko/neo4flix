package sen.dev.movie_service.services.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.uwetrottmann.tmdb2.entities.BaseMovie;
import com.uwetrottmann.tmdb2.entities.Trending;

import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public class Utils {

    public static List<MovieSummaryDTO> mapToMovieSummaryDTOListTrending(
            List<Trending> trendings, Function<List<Integer>, List<String>> genreResolver) {
        return trendings.stream()
                .map(t -> mapToMovieSummaryDTO(t.movie, genreResolver))
                .collect(Collectors.toList());
    }

    public static List<MovieSummaryDTO> mapToMovieSummaryDTOList(
            List<BaseMovie> movies, Function<List<Integer>, List<String>> genreResolver) {
        return movies.stream()
                .map(m -> mapToMovieSummaryDTO(m, genreResolver))
                .collect(Collectors.toList());
    }

    public static MovieSummaryDTO mapToMovieSummaryDTO(
            BaseMovie tmdbMovie, Function<List<Integer>, List<String>> genreResolver) {
        return MovieSummaryDTO.builder()
                .tmdbId(tmdbMovie.id)
                .title(tmdbMovie.title)
                .overview(tmdbMovie.overview)
                .posterPath(tmdbMovie.poster_path)
                .backdropPath(tmdbMovie.backdrop_path)
                .voteAverage(tmdbMovie.vote_average)
                .releaseYear(extractYear(tmdbMovie.release_date))
                .genres(genreResolver.apply(tmdbMovie.genre_ids))
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
