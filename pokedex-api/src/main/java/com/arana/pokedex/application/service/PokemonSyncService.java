package com.arana.pokedex.application.service;

import com.arana.pokedex.application.port.in.SyncPokemonUseCase;
import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.application.port.out.PokemonCatalogPort;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.exception.ValidationException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PokemonDetail;
import com.arana.pokedex.domain.model.PokemonIdentifier;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * User Story 03: copy Pokemon from the catalog into the local store.
 * <p>
 * Sync is an upsert: a new Pokemon is created with empty custom fields, while an
 * existing one gets its catalog fields refreshed and keeps its custom fields.
 */
public class PokemonSyncService implements SyncPokemonUseCase {

    private final PokemonCatalogPort catalog;
    private final LocalPokemonRepositoryPort repository;
    private final Clock clock;

    public PokemonSyncService(PokemonCatalogPort catalog, LocalPokemonRepositoryPort repository, Clock clock) {
        this.catalog = Objects.requireNonNull(catalog);
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public SyncResult sync(PokemonIdentifier identifier) {
        PokemonDetail detail = catalog.findDetail(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Pokemon", identifier));
        return upsert(detail);
    }

    @Override
    public List<SyncResult> syncRange(int fromId, int toId) {
        if (fromId < 1) {
            throw new ValidationException("fromId", "must be greater than or equal to 1");
        }
        if (toId < fromId) {
            throw new ValidationException("toId", "must be greater than or equal to fromId");
        }
        if (toId - fromId + 1 > MAX_RANGE) {
            throw new ValidationException("toId", "a range can contain at most " + MAX_RANGE + " Pokemon");
        }

        // Fetch everything first, so an unknown id fails the whole request before anything is written.
        List<PokemonDetail> details = IntStream.rangeClosed(fromId, toId)
                .mapToObj(PokemonIdentifier::of)
                .map(id -> catalog.findDetail(id).orElseThrow(() -> new ResourceNotFoundException("Pokemon", id)))
                .toList();

        List<SyncResult> results = new ArrayList<>(details.size());
        for (PokemonDetail detail : details) {
            results.add(upsert(detail));
        }
        return results;
    }

    private SyncResult upsert(PokemonDetail detail) {
        return repository.findById(detail.id())
                .map(existing -> {
                    existing.refreshFromCatalog(detail, clock.instant());
                    return new SyncResult(repository.save(existing), false);
                })
                .orElseGet(() -> new SyncResult(repository.save(LocalPokemon.fromCatalog(detail, clock.instant())), true));
    }
}
