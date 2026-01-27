package sen.dev.movie_service.data.repositories;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import sen.dev.movie_service.data.entities.GenreEntity;

public interface GenreRepository extends Neo4jRepository<GenreEntity, Long> {

    // Find genre by the TMDB ID
    Optional<GenreEntity> findByTmdbId(Integer tmdbId);
}
