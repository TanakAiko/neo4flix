package sen.dev.movie_service.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.uwetrottmann.tmdb2.entities.BaseMovie;
import com.uwetrottmann.tmdb2.entities.Trending;

import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public class Utils {

    public static List<MovieSummaryDTO> mapToDTOListTrending(List<Trending> movies) {
        return movies.stream()
                .map(Utils::mapToMovieSummaryDTOTending)
                .collect(Collectors.toList());
    }

    private static MovieSummaryDTO mapToMovieSummaryDTOTending(Trending tmdbMovie) {
        return mapToMovieSummaryDTO(tmdbMovie.movie);
    }

    public static List<MovieSummaryDTO> mapToDTOListPopular(List<BaseMovie> movies) {
        return movies.stream()
                .map(Utils::mapToMovieSummaryDTO)
                .collect(Collectors.toList());
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

    private static Integer extractYear(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .getYear();
    }
}
