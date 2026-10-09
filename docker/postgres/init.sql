-- POSTGRES_DB already creates "pokedex" (pokedex-api's database).
-- pokedex-auth gets its own database: each service owns its data, and only
-- pokedex-auth can read the users table.
CREATE DATABASE pokedex_auth;
