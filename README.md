# Pokédex: full-stack technical exercise

A Pokémon app built on top of [PokeAPI](https://pokeapi.co/docs/v2):
browse the catalog, view details and evolution chains, replicate Pokémon into a local database,
and enrich them with your own data (localized names, regions, tags, notes).

**Stack:** Java 21 · Spring Boot 3.5 · Spring Cloud Gateway · Spring Security (OAuth2 / JWT) · JPA + Flyway ·
PostgreSQL (H2 for zero-setup runs) · Caffeine cache · React 19 + TypeScript + TanStack Query · Docker.

---

## Contents

1. [Architecture](#architecture)
2. [User stories](#user-stories)
3. [Quick start](#quick-start)
4. [Accounts](#accounts)
5. [API reference](#api-reference)
6. [Security](#security)
7. [Error handling](#error-handling)
8. [Project structure](#project-structure)
9. [Design decisions and trade-offs](#design-decisions-and-trade-offs)
10. [GenAI](#genai)

---

## Architecture

Four independent projects, each with its own build, tests and Dockerfile:

| Project | Port | Responsibility |
|---|---|---|
| [`pokedex-web`](pokedex-web) | 5173 (dev) / 3000 (Docker) | React SPA. Talks only to the gateway. |
| [`pokedex-gateway`](pokedex-gateway) | **8080** | Single entry point: routing, **local JWT validation** (JWKS), access rules, CORS, status-code mapping, correlation ids. Stateless, no database. |
| [`pokedex-auth`](pokedex-auth) | 8082 | Authorization server: users, registration, password checks, **token issuing** (RS256), JWKS. |
| [`pokedex-api`](pokedex-api) | 8081 | Resource server: PokeAPI integration, local replica (CRUD), validates JWTs again. |

```
                          ┌─────────────────────────────────────┐
             /oauth2/**   │ pokedex-auth  :8082                 │      ┌──────────────────┐
             /auth/**     │ • POST /auth/register               │──────│ pokedex_auth DB  │
          ┌──────────────►│ • POST /oauth2/token  (signs JWT)   │      │ app_user         │
          │               │ • GET  /oauth2/jwks   (public key)  │      └──────────────────┘
          │               └──────────▲──────────────────▲───────┘
          │                          │ fetch + cache     │ fetch + cache
          │                          │ JWKS              │ JWKS
┌─────────┴────────┐       ┌─────────┴──────────┐        │
│ pokedex-web      │──────►│ pokedex-gateway    │        │
│ React SPA        │ all   │ :8080              │        │
│ :5173 / :3000    │ calls │ • routing          │        │
└──────────────────┘       │ • JWT filter       │        │
                           │ • access rules     │        │
                           │ • CORS             │        │
                           │ • 401/403/503/504  │        │
                           └─────────┬──────────┘        │
                                     │ /api/**           │
                                     │ (valid token only)│
                                     ▼                   │
                           ┌────────────────────────┐    │       ┌──────────────────┐
                           │ pokedex-api  :8081     │────┘       │ pokedex DB       │
                           │ Clean Architecture     │────────────│ local_pokemon    │
                           │ resource server        │            └──────────────────┘
                           │ (validates JWT again)  │
                           └───────────┬────────────┘
                                       │ HTTPS, cached (Caffeine)
                                       │ /pokemon, /pokemon-species,
                                       │ /evolution-chain
                                       ▼
                           ┌────────────────────────┐
                           │ PokeAPI  (external)    │
                           │ https://pokeapi.co     │
                           │ public, read-only      │
                           └────────────────────────┘
```

### Request flow

```
1. Register   POST /auth/register   ──gateway──► pokedex-auth   validates + stores BCrypt hash
2. Login      POST /oauth2/token    ──gateway──► pokedex-auth   finds user, checks password, signs JWT
3. Use        PUT  /api/...  Bearer ──gateway──► JWT filter: signature (cached JWKS), iss, aud, exp, role
                                                   ├─ invalid  → 401 / 403, never reaches the API
                                                   └─ valid    → pokedex-api validates again → 200
```

The gateway and the API never call the auth server per request: they download its **public** keys once
(`/oauth2/jwks`), cache them, and verify signatures locally.

### Clean Architecture (pokedex-api and pokedex-auth)

```
infrastructure  (web controllers, JPA adapters, PokeAPI client, security, config)
      │  depends on
      ▼
application     (use cases + ports: interfaces the infrastructure implements)
      │  depends on
      ▼
domain          (models and business rules: plain Java, no Spring, no JPA, no Jackson)
```

- Use-case services are plain Java classes, wired in a `@Configuration` composition root (`UseCaseConfig`).
- Ports (`PokemonCatalogPort`, `LocalPokemonRepositoryPort`, `TokenSignerPort`…) invert the dependencies:
  the domain doesn't know about HTTP, SQL or JWT.
- **ArchUnit tests enforce it** (`CleanArchitectureTest`): the build fails if the domain imports Spring,
  or a controller reaches into a repository.

---

## User stories

| Story | What | Where |
|---|---|---|
| **US01: Browse** | Paginated list with sprite, category (genus), weight and abilities. **Cached** (Caffeine, plus `Cache-Control` for the browser). | `GET /api/pokemon` · `PokemonCatalogService` · `PokeApiCatalogAdapter` · web: *Browse* |
| **US02: Detail** | Official artwork, base stats, description, **evolution chain** (including branches such as Eevee). | `GET /api/pokemon/{idOrName}` · `PokeApiMapper.flattenEvolutionChain` · web: detail page |
| **US03: Sync** | Replicate one Pokémon or a range (max 50) into the local DB. Upsert: refreshes PokeAPI fields, **keeps custom fields**. | `POST /api/local-pokemon/sync[/range]` · `PokemonSyncService` · web: *My Pokedex* |
| **US04: Update** | Edit proprietary fields (localized name, region, tags, notes). **404** missing, **400** malformed or invalid (per-field errors), **409** concurrent edit (optimistic locking). | `PUT /api/local-pokemon/{id}` · `LocalPokemon.updateCustomFields` · web: edit form |

**How the PokeAPI integration works:** the PokeAPI list endpoint returns only names, so each page needs
two extra calls per Pokémon (`/pokemon` and `/pokemon-species`). They run in parallel on **virtual threads**,
and each one is cached, so a page seen before costs zero HTTP calls (about 1.7 s cold, about 80 ms warm).

---

## Quick start

### Option A: Docker (everything)

```bash
docker compose up --build
```

| | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Gateway (all API calls) | http://localhost:8080 |
| API docs (Swagger, through the gateway) | http://localhost:8080/swagger-ui.html |

PostgreSQL holds two databases: `pokedex` (API) and `pokedex_auth` (auth server). Only the gateway and the
frontend are published to the host.

### Option B: local, no Docker (H2 in-memory, zero setup)

Requirements: **JDK 21**, **Maven 3.9+**, **Node 22**.

```bash
# four terminals
cd pokedex-auth    && mvn spring-boot:run      # :8082
cd pokedex-api     && mvn spring-boot:run      # :8081
cd pokedex-gateway && mvn spring-boot:run      # :8080
cd pokedex-web     && npm install && npm run dev   # :5173
```

Open http://localhost:5173. Data is seeded on every start (6 local Pokémon, 2 demo users).

---

## Accounts

| Username | Password | Role | Can |
|---|---|---|---|
| `ash` | `Pikachu123` | USER | browse, sync, edit |
| `admin` | `Admin1234` | ADMIN | everything above, plus **delete** |

New accounts can register from the UI (always as USER).

Seeded local Pokémon: Bulbasaur, Charmander, Squirtle, Pikachu, Eevee, Chikorita, with localized names,
regions and tags.

---

## API reference

All through the gateway at `http://localhost:8080`. Full OpenAPI docs: `/swagger-ui.html`.

### Identity (pokedex-auth)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | public | JSON `{username, email, password}`. **201**, 400 (field errors), 409 (taken) |
| POST | `/oauth2/token` | public | Form `grant_type=password&username=…&password=…`. **200** `{access_token, token_type, expires_in}`; 400 `invalid_grant` / `invalid_request` / `unsupported_grant_type` |
| GET | `/oauth2/jwks` | public | Public signing keys |
| GET | `/.well-known/oauth-authorization-server` | public | RFC 8414 metadata |

### Catalog (pokedex-api, read-only, PokeAPI)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/pokemon?page=0&size=20` | public | US01: page of summaries (size 1-50) |
| GET | `/api/pokemon/{idOrName}` | public | US02: full detail |

### Local Pokémon (pokedex-api, replica in the local DB)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/local-pokemon?page&size&search` | public | List; search by name, localized name or tag |
| GET | `/api/local-pokemon/{id}` | public | One record |
| POST | `/api/local-pokemon/sync` | **USER** | US03: `{idOrName}`. **201** created / 200 refreshed |
| POST | `/api/local-pokemon/sync/range` | **USER** | US03: `{fromId, toId}` (max 50) |
| PUT | `/api/local-pokemon/{id}` | **USER** | US04: `{localizedName, region, tags[], notes, version}` |
| DELETE | `/api/local-pokemon/{id}` | **ADMIN** | **204** |

Ready-made requests:
- **Postman:** [`postman/`](postman). Import the collection and environment, then *Run collection*
  (33 requests, 43 assertions; tokens are captured automatically).
- **IntelliJ HTTP client:** [`pokedex-gateway/http/gateway.http`](pokedex-gateway/http/gateway.http)

---

## Security

| Layer | What it does |
|---|---|
| **pokedex-auth** | BCrypt password hashes; registration rules (8-72 chars, a letter and a digit); same error and the same BCrypt work for "unknown user" and "wrong password" (no username enumeration, no timing leak); RS256 JWT with `iss`, `aud`, `exp`, `nbf`, `jti`, `roles`; only the **public** key is published. |
| **pokedex-gateway** | Validates every token locally against the cached JWKS (signature, `iss`, `aud`, `exp`/`nbf`); access rules in config (`PUBLIC` / `AUTHENTICATED` / role), **secure by default** for unmatched paths; strips client-sent `X-User-*` headers, then forwards identity headers; CORS for the SPA. |
| **pokedex-api** | Independent OAuth2 resource server with the same JWKS and rules: requests that bypass the gateway are still rejected (**defense in depth**). |
| **pokedex-web** | Token in `sessionStorage`, auto sign-out at expiry or on any 401; route guards are UX only, since the servers enforce the real rules. |

---

## Error handling

Every service answers errors with the same [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) body
(`application/problem+json`), so the frontend handles them one way:

```json
{
  "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "The request contains invalid values",
  "instance": "/api/local-pokemon/94",
  "timestamp": "2026-10-09T18:38:42Z",
  "errors": { "tags": "'not ok!' must be 1-30 characters of a-z, 0-9 or '-'" }
}
```

| Status | When |
|---|---|
| 400 | Malformed JSON, wrong types, Bean Validation or domain rule violations (with per-field `errors`) |
| 401 | Missing, invalid, forged or expired token (`WWW-Authenticate: Bearer error="invalid_token"`) |
| 403 | Valid token without the required role (`error="insufficient_scope"`) |
| 404 | Unknown Pokémon or local record; unrouted path at the gateway |
| 409 | Username/email taken; stale `version` on update (optimistic locking) |
| 415 | Wrong content type |
| 502 | PokeAPI failed (API) |
| 503 / 504 | A backend service is down / too slow (mapped at the gateway instead of a generic 500) |
| 500 | Unexpected: logged with details, generic message to the client |

The token endpoint follows OAuth2's own error format (RFC 6749 §5.2): `{"error": "invalid_grant", ...}`.

---

## Project structure

```
pokedex-challenge/
├── pokedex-api/            Spring Boot resource server
│   └── src/main/java/com/arana/pokedex/
│       ├── domain/              model (LocalPokemon, PokemonDetail…), exceptions
│       ├── application/         port/in (use cases), port/out (ports), service
│       └── infrastructure/      web, persistence, pokeapi, security, config
├── pokedex-auth/           Spring Boot authorization server (same layering)
├── pokedex-gateway/        Spring Cloud Gateway (WebFlux)
│   ├── src/main/java/com/arana/gateway/
│   │   ├── security/            JwtAuthenticationFilter, AccessRules
│   │   └── web/                 GatewayErrorHandler, CorsConfig, CorrelationIdFilter
│   └── http/gateway.http        manual requests
├── pokedex-web/            React + Vite + TypeScript
│   └── src/  api/ · auth/ · hooks/ (TanStack Query) · components/ · pages/ · utils/
├── postman/                collection + environment
├── docker/postgres/        DB init script
├── docker-compose.yml
└── docs/GENAI.md           GenAI exercise
```

---

## Design decisions and trade-offs

| Decision | Why | Trade-off |
|---|---|---|
| **Separate auth server** | One owner for identity; the gateway stays stateless; services share only a *public* JWKS, never a secret | One more service to run |
| **Validate JWTs at the gateway *and* the API** | Reject bad traffic at the edge; the API stays safe if reached directly | Validation runs twice (cheap: local signature check) |
| **JWKS instead of token introspection** | No call to the auth server per request; keeps working while it is briefly down | A token can't be revoked before it expires (mitigated by a 1 h TTL) |
| **Password grant** | First-party SPA, simple demo login | Deprecated in OAuth 2.1; production → Authorization Code + PKCE |
| **Pokédex number as primary key** | A Pokémon can be replicated only once; sync is a natural upsert | Can't store custom Pokémon not in PokeAPI (by design) |
| **Create = sync from PokeAPI** | A local record always mirrors a real Pokémon; the user adds only proprietary fields | No free-form create endpoint |
| **Optimistic locking (`version`)** | Two people editing the same Pokémon: the second gets 409 instead of silently overwriting | Client must send `version` (optional; omitted = last write wins) |
| **Caffeine cache on PokeAPI calls** | Data is effectively static; respects PokeAPI's fair-use policy | Per-instance cache (Redis for multiple replicas) |
| **Virtual threads for page enrichment** | N parallel blocking calls without a thread pool to tune | Requires Java 21 |
| **H2 by default, PostgreSQL in Docker** | Clone-and-run with zero setup; same Flyway SQL for both | H2 in PostgreSQL mode isn't 100% identical |
| **TanStack Query for server state** | Caching, de-duplication, retries, invalidation after mutations; little hand-written state | Extra dependency |
| **Token in `sessionStorage`** | Survives reloads, cleared on tab close | XSS-readable; a BFF with httpOnly cookies is stronger |

---

## GenAI

The GenAI exercise (task-management API prompt, output, validation and corrections) and how AI tools were used
in this project are documented in **[docs/GENAI.md](docs/GENAI.md)**.
