package com.arana.pokedex.infrastructure.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * Stores a short list of PokeAPI slugs (types, abilities) as a comma-separated column.
 * Slugs never contain commas, and these lists are read-only copies we never query by.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(Arrays.asList(column.split(",")));
    }
}
