package com.arana.pokedex.domain.exception;

import java.util.Map;

/**
 * A business rule was violated by the input. Maps to HTTP 400.
 */
public class ValidationException extends DomainException {

    private final Map<String, String> errors;

    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed: " + errors);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
