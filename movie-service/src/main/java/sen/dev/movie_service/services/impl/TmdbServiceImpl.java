package sen.dev.movie_service.services.impl;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.GenreResults;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.entities.TrendingResultsPage;
import com.uwetrottmann.tmdb2.enumerations.MediaType;
import com.uwetrottmann.tmdb2.enumerations.TimeWindow;
import com.uwetrottmann.tmdb2.services.MoviesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import retrofit2.Response;
import sen.dev.movie_service.data.entities.GenreEntity;
import sen.dev.movie_service.data.entities.MovieEntity;
import sen.dev.movie_service.data.entities.PersonEntity;
import sen.dev.movie_service.data.repositories.GenreRepository;
import sen.dev.movie_service.data.repositories.PersonRepository;
import sen.dev.movie_service.services.TmdbService;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TmdbServiceImpl implements TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbServiceImpl.class);

    private final Tmdb tmdb;
    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;

    /** Lazy-loaded cache: TMDB genre ID → genre name. */
    private final Map<Integer, String> genreCache = new ConcurrentHashMap<>();

    public TmdbServiceImpl(@Value("${tmdb.api.key}") String tmdbApiKey,
            GenreRepository genreRepository,
            PersonRepository personRepository) {
        this.tmdb = new Tmdb(tmdbApiKey);
        this.genreRepository = genreRepository;
        this.personRepository = personRepository;
    }

    /**
     * Returns genre names for the given TMDB genre IDs.
     * The genre map is fetched once from TMDB and cached in memory.
     */
    public List<String> resolveGenreNames(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }
        ensureGenreCacheLoaded();
        return genreIds.stream()
                .map(id -> genreCache.getOrDefault(id, "Unknown"))
                .collect(Collectors.toList());
    }

    private void ensureGenreCacheLoaded() {
        if (!genreCache.isEmpty()) {
            return;
        }
        try {
            Response<GenreResults> response = tmdb.genreService().movie("en-US").execute();
            if (response.isSuccessful() && response.body() != null && response.body().genres != null) {
                for (Genre g : response.body().genres) {
                    genreCache.put(g.id, g.name);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load TMDB genre list: {}", e.getMessage());
        }
    }

    @Override
    public List<MovieSummaryDTO> fetchTrendingMovies() throws IOException {

        Response<TrendingResultsPage> response = tmdb.trendingService()
                .trending(MediaType.MOVIE, TimeWindow.DAY)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to fetch trending movies from TMDB");
        }

        return Utils.mapToMovieSummaryDTOListTrending(response.body().results, this::resolveGenreNames);

    }

    @Override
    public List<MovieSummaryDTO> fetchPopularMovies() throws IOException {
        // Use TMDB's "top_rated" endpoint for all-time highest rated movies
        Response<MovieResultsPage> response = tmdb.moviesService()
                .topRated(1, "en-US", null)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to fetch all-time popular movies from TMDB");
        }

        return Utils.mapToMovieSummaryDTOList(response.body().results, this::resolveGenreNames);
    }

    @Override
    public List<MovieSummaryDTO> searchMovies(String title) throws IOException {
        Response<MovieResultsPage> response = tmdb.searchService()
                .movie(title, 1, "en-US", null, false, null, null)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to search movies from TMDB");
        }

        return Utils.mapToMovieSummaryDTOList(response.body().results, this::resolveGenreNames);
    }

    @Override
    public MovieEntity fetchAndMapMovieDetails(Integer tmdbId) throws IOException {
        MoviesService moviesService = tmdb.moviesService();

        // 1. Get Basic Movie Details
        Response<Movie> movieResponse = moviesService.summary(tmdbId, "en-US").execute();

        if (!movieResponse.isSuccessful() || movieResponse.body() == null) {
            throw new RuntimeException("Failed to fetch movie details from TMDB for ID: " + tmdbId);
        }

        Movie tmdbMovie = movieResponse.body();

        // 2. Get Credits (Cast & Crew)
        Response<Credits> creditsResponse = moviesService.credits(tmdbId).execute();
        Credits credits = (creditsResponse.isSuccessful()) ? creditsResponse.body() : new Credits();

        // 3. Map to our Entity
        return mapToEntity(tmdbMovie, credits);
    }

    @Override
    public List<MovieSummaryDTO> fetchSimilarMovies(Integer tmdbId) throws IOException {
        Response<MovieResultsPage> response = tmdb.moviesService()
                .similar(tmdbId, 1, "en-US")
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            return List.of();
        }

        return Utils.mapToMovieSummaryDTOList(response.body().results, this::resolveGenreNames);
    }

    private MovieEntity mapToEntity(Movie tmdbMovie, Credits credits) {
        MovieEntity.MovieEntityBuilder builder = MovieEntity.builder()
                .tmdbId(tmdbMovie.id)
                .title(tmdbMovie.title)
                .overview(tmdbMovie.overview)
                .releaseDate(Utils.convertToLocalDate(tmdbMovie.release_date))
                .posterPath(tmdbMovie.poster_path)
                .backdropPath(tmdbMovie.backdrop_path)
                .voteAverage(tmdbMovie.vote_average);

        // --- Map Genres ---
        Set<GenreEntity> genreEntities = new HashSet<>();
        if (tmdbMovie.genres != null) {
            for (Genre g : tmdbMovie.genres) {
                genreEntities.add(findOrCreateGenre(g.id, g.name));
            }
        }
        builder.genres(genreEntities);

        // --- Map Directors ---
        Set<PersonEntity> directors = new HashSet<>();
        if (credits.crew != null) {
            directors = credits.crew.stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job))
                    .map(c -> findOrCreatePerson(c.id, c.name, c.profile_path))
                    .collect(Collectors.toSet());
        }
        builder.directors(directors);

        // --- Map Cast (Top 5) ---
        Set<PersonEntity> cast = new HashSet<>();
        if (credits.cast != null) {
            cast = credits.cast.stream()
                    .limit(5)
                    .map(c -> findOrCreatePerson(c.id, c.name, c.profile_path))
                    .collect(Collectors.toSet());
        }
        builder.cast(cast);

        return builder.build();
    }

    @Transactional
    public GenreEntity findOrCreateGenre(Integer tmdbId, String name) {
        return genreRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> genreRepository.save(
                        GenreEntity.builder()
                                .tmdbId(tmdbId)
                                .name(name)
                                .build()));
    }

    @Transactional
    public PersonEntity findOrCreatePerson(Integer tmdbId, String name, String profilePath) {
        return personRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> personRepository.save(
                        PersonEntity.builder()
                                .tmdbId(tmdbId)
                                .name(name)
                                .profilePath(profilePath)
                                .build()));
    }
}