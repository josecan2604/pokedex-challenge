package com.arana.pokedex.domain.model;

import com.arana.pokedex.domain.exception.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

public record PokemonIdentifier(String value) {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,49}$");

    public PokemonIdentifier {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!VALID.matcher(normalized).matches()) {
            throw new ValidationException("idOrName", "must be a Pokedex number or a name like 'mr-mime'");
        }
        if (normalized.matches("\\d+")) {
            int id = normalized.length() > 9 ? 0 : Integer.parseInt(normalized);
            if (id <= 0) {
                throw new ValidationException("idOrName", "must be a positive Pokedex number");
            }
            normalized = String.valueOf(id);
        }
        value = normalized;
    }

    public static PokemonIdentifier of(String value) {
        return new PokemonIdentifier(value);
    }

    public static PokemonIdentifier of(int id) {
        return new PokemonIdentifier(String.valueOf(id));
    }

    @Override
    public String toString() {
        return value;
    }
}
