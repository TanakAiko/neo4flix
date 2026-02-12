package sn.dev.user_service.data.entities;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("User")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String keycloakId;

    @Property("username")
    private String username;

    @Property("email")
    private String email;

    @Property("firstname")
    private String firstname;

    @Property("lastname")
    private String lastname;

    /**
     * Base32-encoded TOTP secret for two-factor authentication.
     * Null when 2FA is not enabled.
     */
    @Property("totpSecret")
    private String totpSecret;
}
