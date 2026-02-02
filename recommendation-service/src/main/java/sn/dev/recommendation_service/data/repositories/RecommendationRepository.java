package sn.dev.recommendation_service.data.repositories;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // ==================== SHARING RECOMMENDATIONS ====================

    /**
     * Share a movie recommendation with another user.
     * Creates a SHARED_RECOMMENDATION relationship from sender to movie,
     * with metadata about the recipient and message.
     * 
     * Returns the recipient's username if successful, empty if movie or recipient doesn't exist.
     */
    public Optional<String> shareRecommendation(String senderKeycloakId, String recipientUsername, 
                                                 Integer tmdbId, String message) {
        return neo4jClient.query(
                "MATCH (sender:User {keycloakId: $senderKeycloakId}) " +
                "MATCH (recipient:User {username: $recipientUsername}) " +
                "MATCH (m:Movie {tmdbId: $tmdbId}) " +
                "WHERE sender <> recipient " +
                "CREATE (sender)-[:SHARED_RECOMMENDATION {" +
                "   toUserId: recipient.keycloakId, " +
                "   toUsername: recipient.username, " +
                "   message: $message, " +
                "   sharedAt: datetime()" +
                "}]->(m) " +
                "RETURN recipient.username")
            .bind(senderKeycloakId).to("senderKeycloakId")
            .bind(recipientUsername).to("recipientUsername")
            .bind(tmdbId).to("tmdbId")
            .bind(message).to("message")
            .fetchAs(String.class)
            .one();
    }

    /**
     * Get all recommendations shared TO a specific user.
     * Returns movie details along with who shared it and when.
     */
    public List<Map<String, Object>> findReceivedSharedRecommendations(String recipientKeycloakId) {
        Collection<Map<String, Object>> results = neo4jClient.query(
                "MATCH (sender:User)-[s:SHARED_RECOMMENDATION]->(m:Movie) " +
                "WHERE s.toUserId = $recipientKeycloakId " +
                "RETURN m.tmdbId AS tmdbId, " +
                "       m.title AS title, " +
                "       m.posterPath AS posterPath, " +
                "       m.overview AS overview, " +
                "       m.voteAverage AS voteAverage, " +
                "       m.releaseYear AS releaseYear, " +
                "       sender.username AS fromUsername, " +
                "       s.message AS message, " +
                "       s.sharedAt AS sharedAt " +
                "ORDER BY s.sharedAt DESC")
            .bind(recipientKeycloakId).to("recipientKeycloakId")
            .fetch()
            .all();
        return List.copyOf(results);
    }

    /**
     * Get all recommendations shared BY a specific user (sent items).
     */
    public List<Map<String, Object>> findSentSharedRecommendations(String senderKeycloakId) {
        Collection<Map<String, Object>> results = neo4jClient.query(
                "MATCH (sender:User {keycloakId: $senderKeycloakId})-[s:SHARED_RECOMMENDATION]->(m:Movie) " +
                "RETURN m.tmdbId AS tmdbId, " +
                "       m.title AS title, " +
                "       m.posterPath AS posterPath, " +
                "       s.toUsername AS toUsername, " +
                "       s.message AS message, " +
                "       s.sharedAt AS sharedAt " +
                "ORDER BY s.sharedAt DESC")
            .bind(senderKeycloakId).to("senderKeycloakId")
            .fetch()
            .all();
        return List.copyOf(results);
    }

    /**
     * Check if user already shared this specific movie with this recipient.
     * Prevents duplicate shares.
     */
    public boolean hasAlreadyShared(String senderKeycloakId, String recipientUsername, Integer tmdbId) {
        return neo4jClient.query(
                "MATCH (sender:User {keycloakId: $senderKeycloakId})-[s:SHARED_RECOMMENDATION]->(m:Movie {tmdbId: $tmdbId}) " +
                "MATCH (recipient:User {username: $recipientUsername}) " +
                "WHERE s.toUserId = recipient.keycloakId " +
                "RETURN count(s) > 0")
            .bind(senderKeycloakId).to("senderKeycloakId")
            .bind(recipientUsername).to("recipientUsername")
            .bind(tmdbId).to("tmdbId")
            .fetchAs(Boolean.class)
            .one()
            .orElse(false);
    }
}
