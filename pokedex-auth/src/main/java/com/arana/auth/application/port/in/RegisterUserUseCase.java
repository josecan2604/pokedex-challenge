package com.arana.auth.application.port.in;

import com.arana.auth.domain.model.User;

public interface RegisterUserUseCase {

    User register(String username, String email, String rawPassword);
}
