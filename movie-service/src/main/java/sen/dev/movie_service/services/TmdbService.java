package sen.dev.movie_service.services;

import java.io.IOException;
import java.util.List;

import sen.dev.movie_service.data.entities.MovieEntity;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public interface TmdbService {

    // Discovery: Trending (Hot today)
    List<MovieSummaryDTO> fetchTrendingMovies() throws IOException;

    // Discovery: Popular (All time best)
    List<MovieSummaryDTO> fetchPopularMovies() throws IOException;

    // Details: Lazy Load (Save to DB)
    MovieEntity fetchAndMapMovieDetails(Integer tmdbId) throws IOException;
}
