package com.arana.pokedex.application.port.in;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;

public interface ManageLocalPokemonUseCase {

    PageResult<LocalPokemon> list(PageRequest pageRequest, String search);

    LocalPokemon get(int id);

    LocalPokemon updateCustomFields(int id, LocalPokemon.CustomFields fields, Long expectedVersion);

    void delete(int id);
}
