package sn.dev.recommendation_service.data.repositories;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RecommendationRepository extends Neo4jRepository<Object, Long> {

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
    @Query("MATCH (u:User {keycloakId: $userId})-[r1:RATED]->(m:Movie)<-[r2:RATED]-(other:User) " +
            "WHERE other <> u AND r2.score >= 4 " +
            "WITH u, other, count(m) AS sharedMovies " +
            "WHERE sharedMovies >= 1 " +
            "MATCH (other)-[r3:RATED]->(rec:Movie) " +
            "WHERE r3.score >= 4 AND NOT EXISTS((u)-[:RATED]->(rec)) " +
            "RETURN rec.tmdbId AS tmdbId, rec.title AS title, rec.posterPath AS posterPath, " +
            "       avg(r3.score) AS score, count(DISTINCT other) AS recommenders " +
            "ORDER BY recommenders DESC, score DESC " +
            "LIMIT 10")
    List<Map<String, Object>> findCollaborativeFiltering(@Param("userId") String userId);

    /**
     * FIND FAVORITE MOVIE
     * Used to find a "seed" for the TMDB fallback.
     */
    @Query("MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie) " +
            "RETURN m.tmdbId AS tmdbId " +
            "ORDER BY r.score DESC, r.timestamp DESC " +
            "LIMIT 1")
    Map<String, Object> findFavoriteMovie(@Param("userId") String userId);
}
