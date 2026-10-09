package com.arana.pokedex.domain.exception;

/**
 * An upstream dependency (PokeAPI) failed or is unreachable. Maps to HTTP 502.
 */
public class ExternalServiceException extends DomainException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
