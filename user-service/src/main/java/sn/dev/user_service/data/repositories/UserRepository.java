package sn.dev.user_service.data.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import sn.dev.user_service.data.entities.User;

@Repository
public interface UserRepository extends Neo4jRepository<User, String> {

    // Spring generates this query automatically based on the method name!
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // Spring Data Neo4j handles this automatically if you name it correctly
    boolean existsByUsername(String username);

    // Find users where username, firstname, or lastname contains the query
    // (case-insensitive)
    @Query("MATCH (u:User) " +
            "WHERE toLower(u.username) CONTAINS toLower($query) " +
            "OR toLower(u.firstname) CONTAINS toLower($query) " +
            "OR toLower(u.lastname) CONTAINS toLower($query) " +
            "RETURN u LIMIT 20")
    List<User> searchUsers(String query);

    @Query("MATCH (me:User {username: $me}), (target:User {username: $target}) " +
            "MERGE (me)-[r:FOLLOWS]->(target) " +
            "RETURN r")
    void followUser(String me, String target);

    @Query("MATCH (me:User {username: $me})-[r:FOLLOWS]->(target:User {username: $target}) " +
            "DELETE r")
    void unfollowUser(String me, String target);
}