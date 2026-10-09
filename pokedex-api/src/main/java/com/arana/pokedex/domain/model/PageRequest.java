package com.arana.pokedex.domain.model;

import com.arana.pokedex.domain.exception.ValidationException;

/**
 * Validated, zero-based pagination request.
 */
public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageRequest {
        if (page < 0) {
            throw new ValidationException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size", "must be between 1 and " + MAX_SIZE);
        }
    }

    public int offset() {
        return page * size;
    }
}
