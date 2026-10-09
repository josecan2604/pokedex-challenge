package com.arana.pokedex.domain.exception;

/**
 * Base type for every error raised by the domain and application layers.
 * The web layer translates each subtype to an HTTP status.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
