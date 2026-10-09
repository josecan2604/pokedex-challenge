package com.arana.auth.domain.exception;

/**
 * Username or email already registered. Maps to HTTP 409.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
