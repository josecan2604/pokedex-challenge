package com.arana.pokedex.domain.model;

import com.arana.pokedex.domain.exception.ValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User Stories 03 and 04: a Pokemon replicated into the local store.
 * <p>
 * Fields copied from PokeAPI are refreshed on every sync. The proprietary fields
 * (localized name, region, tags, notes) belong to us: a sync never overwrites them,
 * and they can only change through {@link #updateCustomFields(CustomFields, Instant)}, which
 * enforces the business rules.
 */
public final class LocalPokemon {

    public static final int MAX_LOCALIZED_NAME_LENGTH = 100;
    public static final int MAX_REGION_LENGTH = 50;
    public static final int MAX_TAGS = 10;
    public static final int MAX_NOTES_LENGTH = 500;
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,29}$");

    private final int id;
    private String name;
    private String spriteUrl;
    private String category;
    private double weightKg;
    private double heightM;
    private List<String> types;
    private List<String> abilities;

    private String localizedName;
    private String region;
    private Set<String> tags;
    private String notes;

    private Long version;
    private Instant syncedAt;
    private Instant updatedAt;

    private LocalPokemon(int id) {
        if (id <= 0) {
            throw new ValidationException("id", "must be a positive Pokedex number");
        }
        this.id = id;
        this.tags = new LinkedHashSet<>();
    }

    /**
     * Creates a new local record from PokeAPI data, with empty custom fields.
     */
    public static LocalPokemon fromCatalog(PokemonDetail detail, Instant now) {
        LocalPokemon pokemon = new LocalPokemon(detail.id());
        pokemon.refreshFromCatalog(detail, now);
        pokemon.updatedAt = now;
        return pokemon;
    }

    /**
     * Rebuilds an existing record from storage. Used only by persistence adapters.
     */
    public static LocalPokemon restore(int id, String name, String spriteUrl, String category,
                                       double weightKg, double heightM, List<String> types,
                                       List<String> abilities, CustomFields customFields,
                                       Long version, Instant syncedAt, Instant updatedAt) {
        LocalPokemon pokemon = new LocalPokemon(id);
        pokemon.name = name;
        pokemon.spriteUrl = spriteUrl;
        pokemon.category = category;
        pokemon.weightKg = weightKg;
        pokemon.heightM = heightM;
        pokemon.types = List.copyOf(types);
        pokemon.abilities = List.copyOf(abilities);
        pokemon.localizedName = customFields.localizedName();
        pokemon.region = customFields.region();
        pokemon.tags = new LinkedHashSet<>(customFields.tags());
        pokemon.notes = customFields.notes();
        pokemon.version = version;
        pokemon.syncedAt = syncedAt;
        pokemon.updatedAt = updatedAt;
        return pokemon;
    }

    /**
     * Copies the PokeAPI-owned fields, leaving the custom fields untouched.
     */
    public void refreshFromCatalog(PokemonDetail detail, Instant now) {
        if (detail.id() != id) {
            throw new ValidationException("id", "catalog data belongs to Pokemon #" + detail.id() + ", not #" + id);
        }
        this.name = detail.name();
        this.spriteUrl = detail.spriteUrl();
        this.category = detail.category();
        this.weightKg = detail.weightKg();
        this.heightM = detail.heightM();
        this.types = List.copyOf(detail.types());
        this.abilities = List.copyOf(detail.abilities());
        this.syncedAt = now;
    }

    /**
     * Replaces the proprietary fields after validating and normalizing them.
     * Blank strings are stored as null; tags are trimmed, lower-cased and de-duplicated.
     *
     * @throws ValidationException listing every rule that failed
     */
    public void updateCustomFields(CustomFields fields, Instant now) {
        Map<String, String> errors = new LinkedHashMap<>();

        String newLocalizedName = normalize(fields.localizedName());
        if (newLocalizedName != null && newLocalizedName.length() > MAX_LOCALIZED_NAME_LENGTH) {
            errors.put("localizedName", "must be at most " + MAX_LOCALIZED_NAME_LENGTH + " characters");
        }

        String newRegion = normalize(fields.region());
        if (newRegion != null && newRegion.length() > MAX_REGION_LENGTH) {
            errors.put("region", "must be at most " + MAX_REGION_LENGTH + " characters");
        }

        Set<String> newTags = new LinkedHashSet<>();
        for (String tag : fields.tags()) {
            String normalizedTag = normalize(tag);
            if (normalizedTag == null) {
                errors.put("tags", "must not contain blank values");
                continue;
            }
            normalizedTag = normalizedTag.toLowerCase();
            if (!TAG_PATTERN.matcher(normalizedTag).matches()) {
                errors.put("tags", "'" + normalizedTag + "' must be 1-30 characters of a-z, 0-9 or '-'");
            }
            newTags.add(normalizedTag);
        }
        if (newTags.size() > MAX_TAGS) {
            errors.put("tags", "must contain at most " + MAX_TAGS + " tags");
        }

        String newNotes = normalize(fields.notes());
        if (newNotes != null && newNotes.length() > MAX_NOTES_LENGTH) {
            errors.put("notes", "must be at most " + MAX_NOTES_LENGTH + " characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        this.localizedName = newLocalizedName;
        this.region = newRegion;
        this.tags = newTags;
        this.notes = newNotes;
        this.updatedAt = now;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public CustomFields customFields() {
        return new CustomFields(localizedName, region, List.copyOf(tags), notes);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpriteUrl() { return spriteUrl; }
    public String getCategory() { return category; }
    public double getWeightKg() { return weightKg; }
    public double getHeightM() { return heightM; }
    public List<String> getTypes() { return types; }
    public List<String> getAbilities() { return abilities; }
    public String getLocalizedName() { return localizedName; }
    public String getRegion() { return region; }
    public Set<String> getTags() { return Set.copyOf(tags); }
    public String getNotes() { return notes; }
    public Long getVersion() { return version; }
    public Instant getSyncedAt() { return syncedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocalPokemon other && id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * The proprietary, user-editable fields.
     */
    public record CustomFields(String localizedName, String region, List<String> tags, String notes) {

        public static final CustomFields EMPTY = new CustomFields(null, null, List.of(), null);

        public CustomFields {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}
