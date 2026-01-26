package sen.dev.movie_service.data.repositories;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import sen.dev.movie_service.data.entities.Person;

import java.util.Optional;

public interface PersonRepository extends Neo4jRepository<Person, Long> {

    Optional<Person> findByTmdbId(Integer tmdbId);
}