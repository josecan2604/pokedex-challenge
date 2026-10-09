package com.arana.pokedex.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The primary key is the Pokedex number, so a Pokemon can only be replicated once.
 */
@Entity
@Table(name = "local_pokemon")
public class LocalPokemonEntity {

    @Id
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sprite_url", length = 500)
    private String spriteUrl;

    @Column(length = 100)
    private String category;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Column(name = "height_m", nullable = false)
    private double heightM;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 200)
    private List<String> types = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 500)
    private List<String> abilities = new ArrayList<>();

    @Column(name = "localized_name", length = 100)
    private String localizedName;

    @Column(length = 50)
    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "local_pokemon_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "tag", nullable = false, length = 30)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(length = 500)
    private String notes;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long version;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpriteUrl() { return spriteUrl; }
    public void setSpriteUrl(String spriteUrl) { this.spriteUrl = spriteUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public double getHeightM() { return heightM; }
    public void setHeightM(double heightM) { this.heightM = heightM; }
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
    public List<String> getAbilities() { return abilities; }
    public void setAbilities(List<String> abilities) { this.abilities = abilities; }
    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
