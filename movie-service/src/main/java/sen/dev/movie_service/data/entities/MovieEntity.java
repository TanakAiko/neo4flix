package sen.dev.movie_service.data.entities;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;
import sen.dev.movie_service.web.dto.PersonDTO;

@Node("Movie")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieEntity {

    @Id
    @GeneratedValue
    private Long internalId;

    @Property("tmdbId")
    private Integer tmdbId;

    @Property("title")
    private String title;

    @Property("overview")
    private String overview;

    @Property("releaseDate")
    private LocalDate releaseDate;

    @Property("posterPath")
    private String posterPath;

    @Property("voteAverage")
    private Double voteAverage;

    // --- RELATIONSHIPS ---

    // Genre Relationship
    @Relationship(type = "IN_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<GenreEntity> genres;

    // Director Relationship
    // (Person)-[:DIRECTED]->(Movie) means INCOMING from the Movie's perspective
    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Set<PersonEntity> directors;

    // Cast (Actors) Relationship
    // (Person)-[:ACTED_IN]->(Movie) means INCOMING from the Movie's perspective
    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private Set<PersonEntity> cast;

    public MovieSummaryDTO mapToSummaryDTO() {
        return MovieSummaryDTO.builder()
                .tmdbId(this.getTmdbId())
                .title(this.getTitle())
                .overview(this.getOverview())
                .posterPath(this.getPosterPath())
                .voteAverage(this.getVoteAverage())
                .releaseYear(this.getReleaseDate() != null ? this.getReleaseDate().getYear() : null)
                .build();
    }

    public MovieDetailsDTO mapToDetailsDTO() {
        // Map Genres to List of String
        List<String> genreNames = this.getGenres() != null
                ? this.getGenres().stream().map(g -> g.getName()).collect(Collectors.toList())
                : List.of();

        // Map Directors to List of PersonDTO
        List<PersonDTO> directorDTOs = this.getDirectors() != null
                ? this.getDirectors().stream()
                        .map(p -> new PersonDTO(p.getTmdbId(), p.getName(), p.getProfilePath()))
                        .collect(Collectors.toList())
                : List.of();

        // Map Cast to List of PersonDTO
        List<PersonDTO> castDTOs = this.getCast() != null
                ? this.getCast().stream()
                        .map(p -> new PersonDTO(p.getTmdbId(), p.getName(), p.getProfilePath()))
                        .collect(Collectors.toList())
                : List.of();

        return MovieDetailsDTO.builder()
                .tmdbId(this.getTmdbId())
                .title(this.getTitle())
                .overview(this.getOverview())
                .releaseDate(this.getReleaseDate())
                .posterPath(this.getPosterPath())
                .voteAverage(this.getVoteAverage())
                .genres(genreNames)
                .directors(directorDTOs)
                .cast(castDTOs)
                .build();
    }
}
