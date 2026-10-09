package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;

/**
 * User Story 04 plus the read and delete side of the local CRUD.
 */
public interface ManageLocalPokemonUseCase {

    PageResult<LocalPokemon> list(PageRequest pageRequest, String search);

    LocalPokemon get(int id);

    /**
     * @param expectedVersion when not null, the update is rejected with a conflict
     *                        if the record changed since the client read it
     */
    LocalPokemon updateCustomFields(int id, LocalPokemon.CustomFields fields, Long expectedVersion);

    void delete(int id);
}
