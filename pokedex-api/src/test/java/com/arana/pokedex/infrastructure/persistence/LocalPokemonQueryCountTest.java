package com.arana.pokedex.infrastructure.persistence;

import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.PageRequest;
import com.arana.pokedex.domain.model.PageResult;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LocalPokemonPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LocalPokemonQueryCountTest {

    @Autowired
    LocalPokemonPersistenceAdapter adapter;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Statistics statistics;

    @BeforeEach
    void resetStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void listingAPageDoesNotQueryTagsOncePerPokemon() {
        PageResult<LocalPokemon> page = adapter.findAll(new PageRequest(0, 50), null);

        assertThat(page.content()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(page.content()).allSatisfy(p -> assertThat(p.getTags()).isNotNull());
        // page query + count query + one batched query for all the tags
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void searchingDoesNotQueryTagsOncePerPokemon() {
        PageResult<LocalPokemon> page = adapter.findAll(new PageRequest(0, 50), "starter");

        assertThat(page.content()).hasSize(4);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
