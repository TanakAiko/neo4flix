package sen.dev.movie_service.data.entities;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
