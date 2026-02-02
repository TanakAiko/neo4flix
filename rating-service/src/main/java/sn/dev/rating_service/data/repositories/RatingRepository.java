package sn.dev.rating_service.data.repositories;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing RATED relationships between User and Movie nodes.
 * 
 * Uses Neo4jClient directly instead of Neo4jRepository because RATED is a
 * relationship type, not a node entity.
 */
@Repository
public class RatingRepository {

    private final Neo4jClient neo4jClient;

    public RatingRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    // --- CREATE OR UPDATE ---

    /**
     * Merges the RATED relationship between a User and a Movie.
     * 
     * Logic:
     * 1. Find the User and Movie nodes.
     * 2. MERGE ensures we don't create duplicate RATED relationships.
     * 3. SET updates the score and timestamp.
     * 
     * Returns the tmdbId if successful, or empty if the Movie doesn't exist in DB.
     */
    public Optional<Integer> rateMovie(String userId, Integer tmdbId, int score) {
        return neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId}) " +
                "MATCH (m:Movie {tmdbId: $tmdbId}) " +
                "MERGE (u)-[r:RATED]->(m) " +
                "SET r.score = $score, r.timestamp = datetime() " +
                "RETURN m.tmdbId")
            .bind(userId).to("userId")
            .bind(tmdbId).to("tmdbId")
            .bind(score).to("score")
            .fetchAs(Integer.class)
            .one();
    }

    // --- DELETE ---

    /**
     * Deletes a rating and returns the count of deleted relationships.
     * Returns 0 if no rating existed, 1 if successfully deleted.
     */
    public long deleteRating(String userId, Integer tmdbId) {
        return neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) " +
                "DELETE r RETURN count(r) AS deleted")
            .bind(userId).to("userId")
            .bind(tmdbId).to("tmdbId")
            .fetchAs(Long.class)
            .one()
            .orElse(0L);
    }

    // --- READ ---

    /**
     * Fetches all ratings for a specific user.
     * Returns a List of Maps to keep this service decoupled from the Movie Entity.
     */
    public List<Map<String, Object>> findUserRatings(String userId) {
        Collection<Map<String, Object>> results = neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie) " +
                "RETURN m.tmdbId AS tmdbId, " +
                "       m.title AS title, " +
                "       m.posterPath AS posterPath, " +
                "       r.score AS score, " +
                "       r.timestamp AS ratedDate " +
                "ORDER BY r.timestamp DESC")
            .bind(userId).to("userId")
            .fetch()
            .all();
        return List.copyOf(results);
    }

    /**
     * Checks if a user has already rated a specific movie.
     * Returns the score if found, empty otherwise.
     */
    public Optional<Integer> findRatingByUserAndMovie(String userId, Integer tmdbId) {
        return neo4jClient.query(
                "MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) " +
                "RETURN r.score")
            .bind(userId).to("userId")
            .bind(tmdbId).to("tmdbId")
            .fetchAs(Integer.class)
            .one();
    }

    /**
     * Calculates the average rating for a movie across all users.
     * This is a public query - no userId required.
     */
    public Optional<Double> findAverageRatingByTmdbId(Integer tmdbId) {
        return neo4jClient.query(
                "MATCH (:User)-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) " +
                "RETURN avg(r.score)")
            .bind(tmdbId).to("tmdbId")
            .fetchAs(Double.class)
            .one();
    }
}