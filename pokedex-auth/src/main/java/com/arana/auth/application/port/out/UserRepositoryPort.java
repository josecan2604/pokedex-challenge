package com.arana.auth.application.port.out;

import com.arana.auth.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * @throws com.arana.auth.domain.exception.ConflictException if a unique constraint is violated
     */
    User save(User user);
}
