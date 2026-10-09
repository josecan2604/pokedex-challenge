package com.arana.pokedex.domain.exception;

/**
 * The request clashes with the current state (duplicate username, stale version). Maps to HTTP 409.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
