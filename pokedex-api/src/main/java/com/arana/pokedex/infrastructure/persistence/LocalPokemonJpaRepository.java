package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Integer> {

    @Query(value = """
            SELECT p FROM LocalPokemonEntity p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.localizedName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(:search) MEMBER OF p.tags
            """,
            countQuery = """
            SELECT COUNT(p) FROM LocalPokemonEntity p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.localizedName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(:search) MEMBER OF p.tags
            """)
    Page<LocalPokemonEntity> search(@Param("search") String search, Pageable pageable);
}
