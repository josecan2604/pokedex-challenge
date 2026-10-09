INSERT INTO local_pokemon (id, name, sprite_url, category, weight_kg, height_m, types, abilities,
                           localized_name, region, notes, row_version, synced_at, updated_at)
VALUES
    (1, 'bulbasaur', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
     'Seed Pokémon', 6.9, 0.7, 'grass,poison', 'overgrow,chlorophyll',
     'Fushigidane', 'Kanto', 'Classic starter. Recommended for beginners.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'charmander', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png',
     'Lizard Pokémon', 8.5, 0.6, 'fire', 'blaze,solar-power',
     'Hitokage', 'Kanto', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'squirtle', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png',
     'Tiny Turtle Pokémon', 9.0, 0.5, 'water', 'torrent,rain-dish',
     'Zenigame', 'Kanto', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (25, 'pikachu', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png',
     'Mouse Pokémon', 6.0, 0.4, 'electric', 'static,lightning-rod',
     'Pikachu', 'Kanto', 'Franchise mascot.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (133, 'eevee', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png',
     'Evolution Pokémon', 6.5, 0.3, 'normal', 'run-away,adaptability,anticipation',
     NULL, 'Kanto', 'Branching evolution: eight possible forms.', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (152, 'chikorita', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/152.png',
     'Leaf Pokémon', 6.4, 0.9, 'grass', 'overgrow,leaf-guard',
     'Chicorita', 'Johto', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO local_pokemon_tag (pokemon_id, tag)
VALUES
    (1, 'starter'), (1, 'gen-1'),
    (4, 'starter'), (4, 'gen-1'),
    (7, 'starter'), (7, 'gen-1'),
    (25, 'mascot'), (25, 'gen-1'), (25, 'fan-favorite'),
    (133, 'gen-1'), (133, 'branching-evolution'),
    (152, 'starter'), (152, 'gen-2');
