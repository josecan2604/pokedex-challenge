package com.arana.pokedex.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class SyncRequests {

    private SyncRequests() {
    }

    /**
     * @param idOrName Pokedex number or PokeAPI name, e.g. "25" or "pikachu"
     */
    public record SyncOneRequest(@NotBlank String idOrName) {
    }

    public record SyncRangeRequest(@NotNull @Positive Integer fromId, @NotNull @Positive Integer toId) {
    }

    public record SyncRangeResponse(int created, int refreshed, List<LocalPokemonResponse> pokemon) {
    }
}
