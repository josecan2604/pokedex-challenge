package com.arana.pokedex.application.port.out;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;

import java.util.Optional;

public interface LocalPokemonRepositoryPort {

    Optional<LocalPokemon> findById(int id);

    PageResult<LocalPokemon> findAll(PageRequest pageRequest, String search);

    LocalPokemon save(LocalPokemon pokemon);

    boolean existsById(int id);

    void deleteById(int id);
}
