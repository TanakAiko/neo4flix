# Neo4flix: Project Requirements Specification

## 1. Project Overview
Neo4flix is a microservices-based movie recommendation platform. It leverages a graph database (Neo4j) to provide highly connected data insights, a Spring Boot backend for business logic, and an Angular frontend for user interaction.

---

## 2. Technical Stack
- **Frontend:** Angular 15+ (TypeScript, RxJS)
- **Backend:** Spring Boot 3.x (Java 17+)
- **Database:** Neo4j (Graph Database)
- **Data Access:** Spring Data Neo4j (SDN), Neo4j OGM, Cypher Query Language
- **Security:** Spring Security, JWT (JSON Web Tokens), OAuth2, Two-Factor Authentication (2FA)
- **DevOps:** Docker, Docker Compose

---

## 3. Data Modeling Requirements (Neo4j)
The system must implement a graph schema that represents the following entities and relationships:

### Nodes
- **User:** `id`, `username`, `email`, `passwordHash`, `enabled2FA`
- **Movie:** `id`, `title`, `releaseDate`, `description`, `posterUrl`
- **Genre:** `name`
- **Actor/Director:** `name`, `biography`

### Relationships
- `(:User)-[:RATED {rating: float, timestamp: long}]->(:Movie)`
- `(:User)-[:WATCHLIST]->(:Movie)`
- `(:Movie)-[:IN_GENRE]->(:Genre)`
- `(:Movie)-[:ACTED_IN]->(:Actor)`
- `(:User)-[:FOLLOWS]->(:User)`



---

## 4. Microservices Requirements

### A. Movie Microservice
- **Core CRUD:** Create, Read, Update, and Delete movie records.
- **Search:** Filter movies by title (partial match), genre, and release year.
- **Aggregation:** Retrieve a movie's details along with its average rating.

### B. User Microservice
- **Account Management:** User registration, login, and profile updates.
- **Social Graph:** Logic for users to follow/unfollow others.
- **Watchlist Logic:** Management of the `WATCHLIST` relationship in the graph.

### C. Rating Microservice
- **Submission:** Allow users to submit or update a rating (1-5 stars) for a movie.
- **Validation:** Ensure a user can only rate a movie once (subsequent ratings update the existing one).

### D. Recommendation Microservice
- **Graph Interaction:** Use Spring Data Neo4j and Cypher.
- **Algorithms:** Implementation of Recommendation logic:
    - **Collaborative Filtering:** Users who liked this movie also liked...
    - **Content-Based:** Recommendations based on genre and actors.
- **Performance:** Complex Cypher queries must be optimized with proper indexing on IDs and properties.



---

## 5. Security Requirements
- **Authentication:** Stateless authentication using **JWT**.
- **Authorization:** Role-Based Access Control (RBAC) to differentiate between `USER` and `ADMIN`.
- **2FA:** Integration of a second factor (e.g., TOTP or Email Verification).
- **Encryption:** Use **HTTPS/TLS** for all endpoints. Passwords must be hashed using **BCrypt**.
- **Password Policy:** Minimum length, special characters, and numeric requirements.

---

## 6. Functional UI Requirements (Angular)
- **Authentication Pages:** Login and Registration forms with validation.
- **Home Dashboard:** Grid view of movies with a search bar.
- **Movie Details Page:** Displays metadata, average rating, and a "Rate Now" component.
- **Recommendation Hub:** A dedicated section showing personalized movie lists.
- **Watchlist:** A private area for users to view saved movies.
- **Social Sharing:** Feature to "share" a recommendation link with a friend.

---

## 7. Deployment & Testing
- **Dockerization:** Each microservice and the frontend must have a `Dockerfile`.
- **Orchestration:** A `docker-compose.yml` file to spin up:
    - Neo4j Instance
    - 4 Microservices
    - Angular Application (served via Nginx)
- **Testing Suites:**
    - **Unit Testing:** JUnit 5 for service logic.
    - **Integration Testing:** Testcontainers for Neo4j integration tests.
    - **Security Testing:** Verification of JWT expiration and unauthorized access attempts.