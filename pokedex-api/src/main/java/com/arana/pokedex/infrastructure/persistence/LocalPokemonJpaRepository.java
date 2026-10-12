package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Integer>,
        JpaSpecificationExecutor<LocalPokemonEntity> {
}
