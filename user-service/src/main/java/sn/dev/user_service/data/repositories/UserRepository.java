package sn.dev.user_service.data.repositories;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import sn.dev.user_service.data.entities.User;

@Repository
public interface UserRepository extends Neo4jRepository<User, String> {

    // Spring generates this query automatically based on the method name!
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}