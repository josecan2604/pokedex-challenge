package com.arana.pokedex.domain.model;

/**
 * One node of an evolution lineage, flattened.
 *
 * @param stage 1 for the base form, 2 for its evolutions, and so on.
 *              Branching evolutions (e.g. Eevee) share the same stage.
 * @param evolvesFromId id of the previous stage, or null for the base form
 */
public record EvolutionStage(int id, String name, int stage, Integer evolvesFromId, String spriteUrl) {
}
