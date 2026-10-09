package com.arana.auth.domain.exception;

/**
 * Base type for every error raised by the domain and application layers.
 * The web layer translates each subtype to an HTTP status.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
