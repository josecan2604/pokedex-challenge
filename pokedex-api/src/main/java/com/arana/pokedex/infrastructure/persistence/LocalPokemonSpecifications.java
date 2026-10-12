package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

final class LocalPokemonSpecifications {

    private LocalPokemonSpecifications() {
    }

    static Specification<LocalPokemonEntity> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String term = search.trim().toLowerCase(Locale.ROOT);
        return Specification.anyOf(nameContains(term), localizedNameContains(term), hasTag(term));
    }

    static Specification<LocalPokemonEntity> nameContains(String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern(term), '\\');
    }

    static Specification<LocalPokemonEntity> localizedNameContains(String term) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("localizedName")), likePattern(term), '\\');
    }

    static Specification<LocalPokemonEntity> hasTag(String tag) {
        return (root, query, cb) -> cb.isMember(tag, root.get("tags"));
    }

    private static String likePattern(String term) {
        String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
