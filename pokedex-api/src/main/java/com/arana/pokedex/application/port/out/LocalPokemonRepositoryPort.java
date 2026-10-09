package com.arana.pokedex.application.port.out;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;

import java.util.Optional;

public interface LocalPokemonRepositoryPort {

    Optional<LocalPokemon> findById(int id);

    /**
     * @param search optional text matched against name, localized name and tags; null or blank means no filter
     */
    PageResult<LocalPokemon> findAll(PageRequest pageRequest, String search);

    /**
     * Inserts or updates. Implementations must reject a stale {@link LocalPokemon#getVersion()}.
     */
    LocalPokemon save(LocalPokemon pokemon);

    boolean existsById(int id);

    void deleteById(int id);
}
