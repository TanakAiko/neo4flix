package sn.dev.rating_service.data.repositories;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RatingRepository extends Neo4jRepository<Object, Long> {

    // --- CREATE OR UPDATE ---

    /**
     * Merges the RATED relationship between a User and a Movie.
     * 
     * Logic:
     * 1. Find the User and Movie nodes.
     * 2. MERGE ensures we don't create duplicate RATED relationships.
     * 3. SET updates the score and timestamp.
     * 
     * Returns the tmdbId if successful, or null if the Movie doesn't exist in DB.
     */
    @Query("MATCH (u:User {keycloakId: $userId}) " +
            "MATCH (m:Movie {tmdbId: $tmdbId}) " +
            "MERGE (u)-[r:RATED]->(m) " +
            "SET r.score = $score, r.timestamp = datetime() " +
            "RETURN m.tmdbId")
    Integer rateMovie(@Param("userId") String userId,
            @Param("tmdbId") Integer tmdbId,
            @Param("score") int score);

    // --- DELETE ---

    @Query("MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) DELETE r")
    void deleteRating(@Param("userId") String userId, @Param("tmdbId") Integer tmdbId);

    // --- READ ---

    /**
     * Fetches all ratings for a specific user.
     * We return a List of Maps to keep this service decoupled from the Movie Entity
     * class.
     * The Map keys (tmdbId, title, etc.) match the fields in UserRatingDTO.
     */
    @Query("MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie) " +
            "RETURN m.tmdbId AS tmdbId, " +
            "       m.title AS title, " +
            "       m.posterPath AS posterPath, " +
            "       r.score AS score, " +
            "       r.timestamp AS ratedDate " +
            "ORDER BY r.timestamp DESC")
    List<Map<String, Object>> findUserRatings(@Param("userId") String userId);

    /**
     * Checks if a user has already rated a specific movie.
     * Useful for the Frontend to pre-fill the stars.
     */
    @Query("MATCH (u:User {keycloakId: $userId})-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) " +
            "RETURN r.score")
    Integer findRatingByUserAndMovie(@Param("userId") String userId,
            @Param("tmdbId") Integer tmdbId);

    /**
     * Calculates the average rating for a movie across all users.
     * This is a public query - no userId required.
     */
    @Query("MATCH (:User)-[r:RATED]->(m:Movie {tmdbId: $tmdbId}) " +
            "RETURN avg(r.score)")
    Double findAverageRatingByTmdbId(@Param("tmdbId") Integer tmdbId);
}