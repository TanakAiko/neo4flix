package sn.dev.user_service.services.Impl;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepository;
import sn.dev.user_service.services.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void syncUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        if (!userRepository.existsById(keycloakId)) {
            User user = User.builder()
                    .keycloakId(keycloakId)
                    .username(jwt.getClaimAsString("preferred_username"))
                    .email(jwt.getClaimAsString("email"))
                    .firstname(jwt.getClaimAsString("given_name"))
                    .lastname(jwt.getClaimAsString("family_name"))
                    .build();

            userRepository.save(user);
        }
    }
}
