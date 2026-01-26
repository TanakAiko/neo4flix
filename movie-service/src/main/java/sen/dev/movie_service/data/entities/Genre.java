package sen.dev.movie_service.data.entities;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("Genre")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue
    private Long internalId; // Neo4j Internal ID

    // We store the TMDB Genre ID here to ensure uniqueness and mapping
    @Property("tmdbId")
    private Integer tmdbId;

    @Property("name")
    private String name;
}
