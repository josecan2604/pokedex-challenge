package com.arana.auth.domain.exception;

/**
 * Wrong username or password. The message is the same in both cases, so a login
 * attempt cannot reveal which usernames exist. Maps to OAuth2 {@code invalid_grant}.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
