package com.arana.auth.application.port.in;

import com.arana.auth.domain.model.User;

public interface RegisterUserUseCase {

    /**
     * Self-registration always creates a USER; admins are only created by seeding.
     *
     * @throws com.arana.auth.domain.exception.ValidationException on invalid input
     * @throws com.arana.auth.domain.exception.ConflictException   if the username or email is taken
     */
    User register(String username, String email, String rawPassword);
}
