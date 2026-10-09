package com.arana.pokedex.application.service;

import com.arana.pokedex.application.port.in.ManageLocalPokemonUseCase;
import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.domain.exception.ConflictException;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;

import java.time.Clock;
import java.util.Objects;

public class LocalPokemonService implements ManageLocalPokemonUseCase {

    private static final String RESOURCE = "Local Pokemon";

    private final LocalPokemonRepositoryPort repository;
    private final Clock clock;

    public LocalPokemonService(LocalPokemonRepositoryPort repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public PageResult<LocalPokemon> list(PageRequest pageRequest, String search) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return repository.findAll(pageRequest, normalizedSearch);
    }

    @Override
    public LocalPokemon get(int id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    @Override
    public LocalPokemon updateCustomFields(int id, LocalPokemon.CustomFields fields, Long expectedVersion) {
        LocalPokemon pokemon = get(id);
        if (expectedVersion != null && !expectedVersion.equals(pokemon.getVersion())) {
            throw new ConflictException("Local Pokemon '%d' was modified by someone else (current version %d, yours %d)"
                    .formatted(id, pokemon.getVersion(), expectedVersion));
        }
        pokemon.updateCustomFields(fields, clock.instant());
        return repository.save(pokemon);
    }

    @Override
    public void delete(int id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        repository.deleteById(id);
    }
}
