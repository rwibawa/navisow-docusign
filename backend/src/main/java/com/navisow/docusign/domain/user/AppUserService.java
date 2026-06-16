package com.navisow.docusign.domain.user;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser getOrCreateUser(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = Optional.ofNullable(jwt.getClaimAsString("name")).orElse(email);

        return userRepository.findBySubject(subject)
            .map(user -> {
                user.setEmail(email);
                user.setDisplayName(name);
                return userRepository.save(user);
            })
            .orElseGet(() -> userRepository.save(new AppUser(subject, email, name)));
    }
}
