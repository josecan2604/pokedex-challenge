package com.arana.pokedex.domain.exception;

/**
 * The requested resource does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s '%s' was not found".formatted(resource, identifier));
    }
}
