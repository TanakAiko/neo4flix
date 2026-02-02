package sen.dev.movie_service.data.repositories;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import sen.dev.movie_service.data.entities.MovieEntity;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends Neo4jRepository<MovieEntity, Long> {

    // Find movie by TMDB ID (Crucial for Lazy Loading)
    Optional<MovieEntity> findByTmdbId(Integer tmdbId);

    // Search for movies by title (fuzzy match)
    List<MovieEntity> findByTitleContainingIgnoreCase(String title);

    // --- Watchlist Logic (Remembering we use @Query here) ---

    @Query("MATCH (u:User {keycloakId: $userId}) " +
            "MATCH (m:Movie {tmdbId: $tmdbId}) " +
            "MERGE (u)-[r:IN_WATCHLIST]->(m) " +
            "RETURN r")
    void addToWatchlist(@Param("userId") String userId, @Param("tmdbId") Integer tmdbId);

    @Query("MATCH (u:User {keycloakId: $userId})-[r:IN_WATCHLIST]->(m:Movie {tmdbId: $tmdbId}) DELETE r RETURN count(r)")
    int removeFromWatchlist(@Param("userId") String userId, @Param("tmdbId") Integer tmdbId);

    @Query("MATCH (u:User {keycloakId: $userId})-[:IN_WATCHLIST]->(m:Movie {tmdbId: $tmdbId}) RETURN count(m) > 0")
    boolean isInWatchlist(@Param("userId") String userId, @Param("tmdbId") Integer tmdbId);

    @Query("MATCH (u:User {keycloakId: $userId})-[:IN_WATCHLIST]->(m:Movie) RETURN m")
    List<MovieEntity> findWatchlistByUserId(@Param("userId") String userId);
}
