package com.arana.pokedex.domain.model;

public record EvolutionStage(int id, String name, int stage, Integer evolvesFromId, String spriteUrl) {
}
