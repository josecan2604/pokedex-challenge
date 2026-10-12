package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.application.port.out.LocalPokemonRepositoryPort;
import com.arana.pokedex.domain.exception.ConflictException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import com.arana.pokedex.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

@Component
public class LocalPokemonPersistenceAdapter implements LocalPokemonRepositoryPort {

    private final LocalPokemonJpaRepository repository;

    public LocalPokemonPersistenceAdapter(LocalPokemonJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalPokemon> findById(int id) {
        return repository.findById(id).map(LocalPokemonPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LocalPokemon> findAll(PageRequest pageRequest, String search) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size(), Sort.by("id"));
        Page<LocalPokemonEntity> page = repository.findAll(LocalPokemonSpecifications.matchesSearch(search), pageable);
        return new PageResult<>(page.map(LocalPokemonPersistenceAdapter::toDomain).getContent(),
                pageRequest.page(), pageRequest.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public LocalPokemon save(LocalPokemon pokemon) {
        try {
            LocalPokemonEntity saved = repository.saveAndFlush(toEntity(pokemon));
            return toDomain(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException(
                    "Local Pokemon '%d' was modified by someone else, reload and try again".formatted(pokemon.getId()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Local Pokemon '%d' already exists".formatted(pokemon.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(int id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        repository.deleteById(id);
    }

    static LocalPokemon toDomain(LocalPokemonEntity entity) {
        return LocalPokemon.restore(
                entity.getId(),
                entity.getName(),
                entity.getSpriteUrl(),
                entity.getCategory(),
                entity.getWeightKg(),
                entity.getHeightM(),
                entity.getTypes(),
                entity.getAbilities(),
                new LocalPokemon.CustomFields(entity.getLocalizedName(), entity.getRegion(),
                        new ArrayList<>(entity.getTags()), entity.getNotes()),
                entity.getVersion(),
                entity.getSyncedAt(),
                entity.getUpdatedAt());
    }

    static LocalPokemonEntity toEntity(LocalPokemon pokemon) {
        LocalPokemonEntity entity = new LocalPokemonEntity();
        entity.setId(pokemon.getId());
        entity.setName(pokemon.getName());
        entity.setSpriteUrl(pokemon.getSpriteUrl());
        entity.setCategory(pokemon.getCategory());
        entity.setWeightKg(pokemon.getWeightKg());
        entity.setHeightM(pokemon.getHeightM());
        entity.setTypes(new ArrayList<>(pokemon.getTypes()));
        entity.setAbilities(new ArrayList<>(pokemon.getAbilities()));
        LocalPokemon.CustomFields custom = pokemon.customFields();
        entity.setLocalizedName(custom.localizedName());
        entity.setRegion(custom.region());
        entity.setTags(new LinkedHashSet<>(custom.tags()));
        entity.setNotes(custom.notes());
        entity.setVersion(pokemon.getVersion());
        entity.setSyncedAt(pokemon.getSyncedAt());
        entity.setUpdatedAt(pokemon.getUpdatedAt());
        return entity;
    }
}
