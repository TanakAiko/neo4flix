package sen.dev.movie_service.services.impl;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.BaseMovie;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.entities.Person;
import com.uwetrottmann.tmdb2.entities.TrendingResultsPage;
import com.uwetrottmann.tmdb2.enumerations.MediaType;
import com.uwetrottmann.tmdb2.enumerations.TimeWindow;
import com.uwetrottmann.tmdb2.services.MoviesService;

import lombok.RequiredArgsConstructor;

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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TmdbServiceImpl implements TmdbService {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.read.access.token}")
    private String tmdbReadAccessToken;

    private final Tmdb tmdb;
    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;

    public TmdbServiceImpl(String tmdbApiKey, GenreRepository genreRepository, PersonRepository personRepository) {
        this.tmdb = new Tmdb(tmdbApiKey);
        this.genreRepository = genreRepository;
        this.personRepository = personRepository;
    }

    @Override
    public List<MovieSummaryDTO> fetchTrendingMovies() throws IOException {

        Response<TrendingResultsPage> response = tmdb.trendingService()
                .trending(MediaType.MOVIE, TimeWindow.DAY)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to fetch popular movies from TMDB");
        }

        return Utils.mapToDTOListTrending(response.body().results);

    }

    @Override
    public List<MovieSummaryDTO> fetchPopularMovies() throws IOException {

        Response<MovieResultsPage> response = tmdb.moviesService()
                .popular(1, "en-US", null)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to fetch popular movies from TMDB");
        }

        return Utils.mapToDTOListPopular(response.body().results);
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

    private MovieEntity mapToEntity(Movie tmdbMovie, Credits credits) {
        MovieEntity.MovieEntityBuilder builder = MovieEntity.builder()
                .tmdbId(tmdbMovie.id)
                .title(tmdbMovie.title)
                .overview(tmdbMovie.overview)
                .releaseDate(tmdbMovie.release_date != null ? java.time.LocalDate.parse(tmdbMovie.release_date) : null)
                .posterPath(tmdbMovie.poster_path)
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
        Set<Person> cast = new HashSet<>();
        if (credits.cast != null) {
            cast = credits.cast.stream()
                    .limit(5)
                    .map(c -> findOrCreatePerson(c.id, c.name, c.profile_path))
                    .collect(Collectors.toSet());
        }
        builder.cast(cast);

        return builder.build();
    }

    // --- Helper Methods ---

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
    public Person findOrCreatePerson(Integer tmdbId, String name, String profilePath) {
        return personRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> personRepository.save(
                        Person.builder()
                                .tmdbId(tmdbId)
                                .name(name)
                                .profilePath(profilePath)
                                .build()));
    }
}