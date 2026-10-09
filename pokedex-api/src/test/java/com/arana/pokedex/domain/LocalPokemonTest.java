package com.arana.pokedex.domain;

import com.arana.pokedex.TestData;
import com.arana.pokedex.domain.exception.ValidationException;
import com.arana.pokedex.domain.model.LocalPokemon;
import com.arana.pokedex.domain.model.LocalPokemon.CustomFields;
import com.arana.pokedex.domain.model.PokemonDetail;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonTest {

    private static final Instant LATER = TestData.NOW.plusSeconds(60);

    @Nested
    class FromCatalog {

        @Test
        void copiesCatalogFieldsAndStartsWithEmptyCustomFields() {
            LocalPokemon pokemon = LocalPokemon.fromCatalog(TestData.pikachuDetail(), TestData.NOW);

            assertThat(pokemon.getId()).isEqualTo(25);
            assertThat(pokemon.getName()).isEqualTo("pikachu");
            assertThat(pokemon.getCategory()).isEqualTo("Mouse Pokémon");
            assertThat(pokemon.getTypes()).containsExactly("electric");
            assertThat(pokemon.customFields()).isEqualTo(CustomFields.EMPTY);
            assertThat(pokemon.getSyncedAt()).isEqualTo(TestData.NOW);
            assertThat(pokemon.getVersion()).isNull();
        }
    }

    @Nested
    class RefreshFromCatalog {

        @Test
        void updatesCatalogFieldsButKeepsCustomFields() {
            LocalPokemon pokemon = TestData.localPikachu();
            PokemonDetail renamed = TestData.pikachuDetail();

            pokemon.refreshFromCatalog(renamed, LATER);

            assertThat(pokemon.getAbilities()).containsExactly("static", "lightning-rod");
            assertThat(pokemon.getSyncedAt()).isEqualTo(LATER);
            assertThat(pokemon.getLocalizedName()).isEqualTo("Pikachu");
            assertThat(pokemon.getTags()).containsExactly("mascot");
        }

        @Test
        void rejectsDataForAnotherPokemon() {
            LocalPokemon bulbasaur = LocalPokemon.restore(1, "bulbasaur", null, null, 6.9, 0.7,
                    List.of("grass"), List.of(), CustomFields.EMPTY, 0L, TestData.NOW, TestData.NOW);

            assertThatThrownBy(() -> bulbasaur.refreshFromCatalog(TestData.pikachuDetail(), LATER))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    class UpdateCustomFields {

        @Test
        void trimsBlanksToNullAndNormalizesTags() {
            LocalPokemon pokemon = TestData.localPikachu();

            pokemon.updateCustomFields(new CustomFields("  Pikachu-ES  ", "   ", List.of(" Starter ", "starter", "GEN-1"), ""), LATER);

            assertThat(pokemon.getLocalizedName()).isEqualTo("Pikachu-ES");
            assertThat(pokemon.getRegion()).isNull();
            assertThat(pokemon.getNotes()).isNull();
            assertThat(pokemon.customFields().tags()).containsExactly("starter", "gen-1");
            assertThat(pokemon.getUpdatedAt()).isEqualTo(LATER);
        }

        @Test
        void nullTagListClearsTags() {
            LocalPokemon pokemon = TestData.localPikachu();

            pokemon.updateCustomFields(new CustomFields(null, null, null, null), LATER);

            assertThat(pokemon.getTags()).isEmpty();
        }

        @Test
        void reportsEveryBrokenRuleAtOnce() {
            LocalPokemon pokemon = TestData.localPikachu();
            CustomFields invalid = new CustomFields("x".repeat(101), "y".repeat(51), List.of("bad tag!"), "z".repeat(501));

            assertThatThrownBy(() -> pokemon.updateCustomFields(invalid, LATER))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                            .containsOnlyKeys("localizedName", "region", "tags", "notes"));
        }

        @Test
        void rejectsMoreThanTenTags() {
            LocalPokemon pokemon = TestData.localPikachu();
            List<String> tags = IntStream.rangeClosed(1, 11).mapToObj(i -> "tag-" + i).toList();

            assertThatThrownBy(() -> pokemon.updateCustomFields(new CustomFields(null, null, tags, null), LATER))
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors()).containsKey("tags"));
        }

        @Test
        void rejectsBlankTag() {
            LocalPokemon pokemon = TestData.localPikachu();

            assertThatThrownBy(() -> pokemon.updateCustomFields(new CustomFields(null, null, List.of("ok", "  "), null), LATER))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void leavesStateUntouchedWhenValidationFails() {
            LocalPokemon pokemon = TestData.localPikachu();

            assertThatThrownBy(() -> pokemon.updateCustomFields(new CustomFields("New", null, List.of("NOT VALID"), null), LATER))
                    .isInstanceOf(ValidationException.class);

            assertThat(pokemon.getLocalizedName()).isEqualTo("Pikachu");
            assertThat(pokemon.getUpdatedAt()).isEqualTo(TestData.NOW);
        }
    }

    @Test
    void rejectsNonPositiveId() {
        assertThatThrownBy(() -> LocalPokemon.restore(0, "x", null, null, 0, 0, List.of(), List.of(),
                CustomFields.EMPTY, 0L, TestData.NOW, TestData.NOW))
                .isInstanceOf(ValidationException.class);
    }
}
