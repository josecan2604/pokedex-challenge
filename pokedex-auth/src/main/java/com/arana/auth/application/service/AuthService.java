package com.arana.auth.application.service;

import com.arana.auth.application.port.in.IssueTokenUseCase;
import com.arana.auth.application.port.in.RegisterUserUseCase;
import com.arana.auth.application.port.out.PasswordHasherPort;
import com.arana.auth.application.port.out.TokenSignerPort;
import com.arana.auth.application.port.out.UserRepositoryPort;
import com.arana.auth.domain.exception.ConflictException;
import com.arana.auth.domain.exception.InvalidCredentialsException;
import com.arana.auth.domain.model.AccessToken;
import com.arana.auth.domain.model.Role;
import com.arana.auth.domain.model.User;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public class AuthService implements RegisterUserUseCase, IssueTokenUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final TokenSignerPort tokenSigner;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                       TokenSignerPort tokenSigner, Clock clock) {
        this.users = Objects.requireNonNull(users);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenSigner = Objects.requireNonNull(tokenSigner);
        this.clock = Objects.requireNonNull(clock);
        this.dummyHash = passwordHasher.hash("timing-attack-mitigation");
    }

    @Override
    public User register(String username, String email, String rawPassword) {
        String normalizedUsername = User.normalizeUsername(username);
        String normalizedEmail = User.normalizeEmail(email);
        User.validateRegistration(normalizedUsername, normalizedEmail, rawPassword);

        if (users.existsByUsername(normalizedUsername)) {
            throw new ConflictException("Username '%s' is already taken".formatted(normalizedUsername));
        }
        if (users.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email '%s' is already registered".formatted(normalizedEmail));
        }

        User user = new User(null, normalizedUsername, normalizedEmail,
                passwordHasher.hash(rawPassword), Role.USER, clock.instant());
        return users.save(user);
    }

    @Override
    public AccessToken issueForCredentials(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        Optional<User> user = users.findByUsername(User.normalizeUsername(username));
        boolean passwordMatches = passwordHasher.matches(rawPassword, user.map(User::passwordHash).orElse(dummyHash));
        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return tokenSigner.sign(user.get());
    }
}
