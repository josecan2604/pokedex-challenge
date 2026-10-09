-- Works on PostgreSQL and on H2 in PostgreSQL mode.
-- Users are not stored here: pokedex-auth owns them.

-- Primary key is the Pokedex number: a Pokemon can be replicated only once.
CREATE TABLE local_pokemon (
    id             INTEGER          NOT NULL PRIMARY KEY,
    name           VARCHAR(100)     NOT NULL,
    sprite_url     VARCHAR(500),
    category       VARCHAR(100),
    weight_kg      DOUBLE PRECISION NOT NULL,
    height_m       DOUBLE PRECISION NOT NULL,
    types          VARCHAR(200)     NOT NULL,
    abilities      VARCHAR(500)     NOT NULL,
    -- proprietary fields (User Story 03)
    localized_name VARCHAR(100),
    region         VARCHAR(50),
    notes          VARCHAR(500),
    row_version    BIGINT           NOT NULL,
    synced_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_local_pokemon_id CHECK (id > 0)
);

CREATE TABLE local_pokemon_tag (
    pokemon_id INTEGER     NOT NULL,
    tag        VARCHAR(30) NOT NULL,
    CONSTRAINT pk_local_pokemon_tag PRIMARY KEY (pokemon_id, tag),
    CONSTRAINT fk_local_pokemon_tag_pokemon FOREIGN KEY (pokemon_id)
        REFERENCES local_pokemon (id) ON DELETE CASCADE
);

CREATE INDEX ix_local_pokemon_name ON local_pokemon (name);
CREATE INDEX ix_local_pokemon_tag_tag ON local_pokemon_tag (tag);
