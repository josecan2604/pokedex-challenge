package com.arana.pokedex.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s '%s' was not found".formatted(resource, identifier));
    }
}
