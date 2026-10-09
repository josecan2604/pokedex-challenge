package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.TestData;
import com.arana.pokedex.domain.exception.ConflictException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.LocalPokemon.CustomFields;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against H2 with the real Flyway migrations, including the seed data (V2).
 * Not transactional, so optimistic locking behaves as in production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LocalPokemonPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LocalPokemonPersistenceAdapterTest {

    @Autowired
    LocalPokemonPersistenceAdapter adapter;

    @Test
    void seedDataIsLoaded() {
        LocalPokemon pikachu = adapter.findById(25).orElseThrow();

        assertThat(pikachu.getName()).isEqualTo("pikachu");
        assertThat(pikachu.getTypes()).containsExactly("electric");
        assertThat(pikachu.getTags()).contains("mascot", "gen-1");
    }

    @Test
    void searchMatchesNameLocalizedNameOrTag() {
        PageRequest page = new PageRequest(0, 50);

        assertThat(names(adapter.findAll(page, "SAUR"))).containsExactly("bulbasaur");
        assertThat(names(adapter.findAll(page, "hitokage"))).containsExactly("charmander");
        assertThat(names(adapter.findAll(page, "starter"))).contains("bulbasaur", "charmander", "squirtle", "chikorita");
    }

    @Test
    void findAllPaginatesSortedById() {
        PageResult<LocalPokemon> first = adapter.findAll(new PageRequest(0, 2), null);

        assertThat(first.content()).extracting(LocalPokemon::getId).containsExactly(1, 4);
        assertThat(first.totalElements()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void insertUpdateAndDeleteRoundTrip() {
        LocalPokemon created = adapter.save(LocalPokemon.fromCatalog(TestData.detailWithId(9001), TestData.NOW));
        assertThat(created.getVersion()).isZero();

        created.updateCustomFields(new CustomFields("Test", "Lab", List.of("qa"), null), TestData.NOW);
        LocalPokemon updated = adapter.save(created);
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(adapter.findById(9001).orElseThrow().getTags()).containsExactly("qa");

        adapter.deleteById(9001);
        assertThat(adapter.existsById(9001)).isFalse();
    }

    @Test
    void staleVersionIsRejected() {
        LocalPokemon first = adapter.save(LocalPokemon.fromCatalog(TestData.detailWithId(9002), TestData.NOW));
        LocalPokemon staleCopy = adapter.findById(9002).orElseThrow();

        first.updateCustomFields(new CustomFields("first", null, List.of(), null), TestData.NOW);
        adapter.save(first);

        staleCopy.updateCustomFields(new CustomFields("second", null, List.of(), null), TestData.NOW);
        assertThatThrownBy(() -> adapter.save(staleCopy)).isInstanceOf(ConflictException.class);

        adapter.deleteById(9002);
    }

    private static List<String> names(PageResult<LocalPokemon> page) {
        return page.content().stream().map(LocalPokemon::getName).toList();
    }
}
