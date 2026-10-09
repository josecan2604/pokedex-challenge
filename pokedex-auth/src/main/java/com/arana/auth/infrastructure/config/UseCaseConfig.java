package com.arana.auth.infrastructure.config;

import com.arana.auth.application.port.out.PasswordHasherPort;
import com.arana.auth.application.port.out.TokenSignerPort;
import com.arana.auth.application.port.out.UserRepositoryPort;
import com.arana.auth.application.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthService authService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                            TokenSignerPort tokenSigner, Clock clock) {
        return new AuthService(users, passwordHasher, tokenSigner, clock);
    }
}
