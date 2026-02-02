package sn.dev.recommendation_service.data.repositories;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Repository for recommendation queries.
 * 
 * Uses Neo4jClient directly instead of Neo4jRepository because this service
 * only runs read queries and doesn't need entity mapping.
 */
@Repository
public class RecommendationRepository {

    private final Neo4jClient neo4jClient;

    public RecommendationRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * COLLABORATIVE FILTERING QUERY
     * Finds movies rated highly by users who also rated the same movies as 'userId'.
     * 
     * Logic:
     * 1. Find all movies the user has rated
     * 2. Find other users who also rated those same movies highly (score >= 4)
     * 3. Get movies those similar users rated highly that our user hasn't seen
     * 4. Return top 10 sorted by average score
     */
    public List<Map<String, Object>> findCollaborativeFiltering(String userId) {
        Collection<Map<String, Object>> results = neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId})-[r1:RATED]->(m:Movie)<-[r2:RATED]-(other:User) " +
                "WHERE other <> u AND r2.score >= 4 " +
                "WITH u, other, count(m) AS sharedMovies " +
                "WHERE sharedMovies >= 1 " +
                "MATCH (other)-[r3:RATED]->(rec:Movie) " +
                "WHERE r3.score >= 4 AND NOT EXISTS((u)-[:RATED]->(rec)) " +
                "RETURN rec.tmdbId AS tmdbId, rec.title AS title, rec.posterPath AS posterPath, " +
                "       rec.overview AS overview, rec.voteAverage AS voteAverage, rec.releaseYear AS releaseYear, " +
                "       avg(r3.score) AS score, count(DISTINCT other) AS recommenders " +
                "ORDER BY recommenders DESC, score DESC " +
                "LIMIT 10")
            .bind(userId).to("userId")
            .fetch()
            .all();
        return List.copyOf(results);
    }

    /**
     * FIND FAVORITE MOVIE
     * Used to find a "seed" for the TMDB fallback.
     */
    public Map<String, Object> findFavoriteMovie(String userId) {
        return neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie) " +
                "RETURN m.tmdbId AS tmdbId " +
                "ORDER BY r.score DESC, r.timestamp DESC " +
                "LIMIT 1")
            .bind(userId).to("userId")
            .fetch()
            .one()
            .orElse(null);
    }
}
