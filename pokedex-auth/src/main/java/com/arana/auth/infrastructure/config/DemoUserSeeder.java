package com.arana.auth.infrastructure.config;

import com.arana.auth.application.port.out.PasswordHasherPort;
import com.arana.auth.application.port.out.UserRepositoryPort;
import com.arana.auth.domain.model.Role;
import com.arana.auth.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
@ConditionalOnProperty(name = "auth.seed.demo-users", havingValue = "true", matchIfMissing = true)
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private record DemoUser(String username, String email, String password, Role role) {
    }

    private static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("admin", "admin@pokedex.local", "Admin1234", Role.ADMIN),
            new DemoUser("ash", "ash@pokedex.local", "Pikachu123", Role.USER));

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    public DemoUserSeeder(UserRepositoryPort users, PasswordHasherPort passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (DemoUser demo : DEMO_USERS) {
            if (!users.existsByUsername(demo.username())) {
                users.save(new User(null, demo.username(), demo.email(),
                        passwordHasher.hash(demo.password()), demo.role(), clock.instant()));
                log.info("Seeded demo user '{}' ({})", demo.username(), demo.role());
            }
        }
    }
}
