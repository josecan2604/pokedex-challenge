package com.arana.auth.domain.exception;

import java.util.Map;

/**
 * Input broke a business rule. Maps to HTTP 400 with one entry per field.
 */
public class ValidationException extends DomainException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Validation failed: " + errors);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
