# Backend Authentication & Run Persistence Design

**Date:** 2026-06-07
**Status:** Approved
**Scope:** Google Sign-In authentication for the Android app against the Ktor backend (backend-issued JWT session model), plus the `users`, `refresh_tokens`, `runs`, and `run_positions` tables needed to support authenticated, per-user run history.

---

## Context

The backend skeleton spec (`2026-06-01-backend-skeleton-design.md`) explicitly called authentication out of scope. This spec covers what comes next: letting Runner users sign in with their Google account and persist their run history per-user in Postgres, replacing `MockRunRepository`.

---

## Chosen approach: Google Sign-In + backend-issued JWTs

Three options were considered:

- **Build it ourselves** — Android Credential Manager gets a Google ID token; the backend verifies it once, then issues its own access/refresh JWTs for everything after.
- **Managed free-tier auth (Supabase Auth / Firebase Auth)** — least backend code, but reintroduces a Supabase/Firebase dependency for identity, conflicting with the project's direction of treating Supabase as a learning tool only (local Postgres is the real target — see backend plan).
- **Self-hosted identity server (Keycloak / Ory Kratos)** — fully self-contained and open source, but a whole extra service to deploy and operate — overkill at this stage.

**Chosen: build it ourselves.** It keeps everything in the stack already chosen (Ktor, Koin, Exposed, Postgres), avoids re-coupling to a third-party identity vendor, and is a well-trodden pattern. It also leaves room to add more social providers, email+password, or even a self-hosted identity server later without disrupting the app.

---

## 1. High-level flow

```
Android App                          Ktor Backend                      Google
-----------                          ------------                      ------
1. User taps "Sign in with Google"
   → Credential Manager returns
     a Google ID token (JWT)
                                                                    (no call yet —
                                                                     token is self-
                                                                     contained)
2. POST /auth/google
   { idToken }            ────────►
                                     3. Verify idToken signature
                                        against Google's public JWKS
                                        (cached, refreshed periodically)
                                     4. Extract sub/email/name
                                     5. Find-or-create user row
                                        in Postgres
                                     6. Mint access JWT (short-lived)
                                        + refresh token (long-lived)
                          ◄────────  7. Return { accessToken, refreshToken }

8. Store tokens securely
   (EncryptedSharedPreferences
    / DataStore)

9. Subsequent requests:
   Authorization: Bearer <accessJWT>  ────────► validated locally via
                                                 Ktor JWT plugin (no
                                                 network round-trip)

10. When access token expires:
    POST /auth/refresh
    { refreshToken }       ────────► validate + rotate → new pair
```

The key property: **Google is only contacted at step 1 (on-device) and never again** — the backend trusts Google's signature on the ID token at sign-in time, then issues its own credentials for everything after. This keeps the hot path independent of Google's availability.

---

## 2. Data model

Four new tables. `users` and `refresh_tokens` support authentication; `runs` and `run_positions` replace `MockRunRepository` with real per-user persistence (`runs` has a one-to-many relationship with `users`).

### `users`
| column | type | notes |
|---|---|---|
| `id` | UUID (PK) | internal identifier, used everywhere else (runs, etc.) |
| `google_sub` | text, unique | Google's stable account ID (the `sub` claim) — the real link to the identity |
| `email` | text | from the ID token, for display/contact |
| `display_name` | text | from the ID token |
| `created_at` | timestamptz | |

### `refresh_tokens`
| column | type | notes |
|---|---|---|
| `id` | UUID (PK) | |
| `user_id` | UUID (FK → users) | |
| `token_hash` | text | store a hash (SHA-256), never the raw token |
| `expires_at` | timestamptz | e.g. 30 days out |
| `revoked_at` | timestamptz, nullable | set on logout or rotation — enables revocation |
| `created_at` | timestamptz | |

Notes:
- `google_sub` (not email) is the identity anchor — permanent and provider-scoped. Adding more social providers later means a small `identities` table (`user_id`, `provider`, `provider_sub`), not overloading `users`.
- Refresh tokens live in the DB (hashed) so they can be revoked ("log out everywhere", compromised device). Access JWTs stay stateless (signature-checked only) since they're short-lived.
- Access JWTs carry `sub = users.id` plus standard `iat`/`exp` claims — nothing sensitive, since JWTs are readable (not encrypted) by anyone holding them.

### `runs` (one-to-many with `users`)

Maps from the Android `RunActivity` data class — but stores typed values instead of `RunActivity`'s display-formatted `String` fields, so the backend can query/aggregate (sum distance, sort by pace, etc.) and the API/client format for display, same as today.

| column | type | maps from | notes |
|---|---|---|---|
| `id` | UUID (PK) | `id` | drop-in replacement for the client's `String` id |
| `user_id` | UUID (FK → users, NOT NULL) | — | one-to-many: a user has many runs |
| `started_at` | timestamptz | `date` | sortable/queryable real timestamp |
| `duration_seconds` | integer | `duration` | seconds, not formatted "mm:ss" |
| `distance_km` | numeric(6,3) | `distanceKm` | precise distance math |
| `pace_sec_per_km` | integer | `paceMinKm` | consistent with `paceSecPerKm` already used in `LocationViewModel` |
| `created_at` | timestamptz | — | row creation bookkeeping |

### `run_positions` (one-to-many with `runs`)
| column | type | maps from | notes |
|---|---|---|---|
| `id` | bigserial (PK) | — | |
| `run_id` | UUID (FK → runs, NOT NULL) | — | one-to-many: a run has many positions |
| `seq` | integer | (list order) | preserves route point order (ordered polyline) |
| `lat` | double precision | `Position.lat` | |
| `lon` | double precision | `Position.lon` | |

---

## 3. Endpoints & components

New routes (under `routes/`, registered in `Routing.kt` like `HealthRoute`):

| Route | Auth? | Purpose |
|---|---|---|
| `POST /auth/google` | No | Verify Google ID token, find-or-create user, return `{ accessToken, refreshToken }` |
| `POST /auth/refresh` | No (carries refresh token in body) | Validate + rotate refresh token, return a new pair |
| `POST /auth/logout` | Yes | Revoke the caller's refresh token(s) |
| `GET /runs` | Yes | List the authenticated user's runs |
| `GET /runs/{id}` | Yes | Fetch one run (with positions) — must belong to the caller |

New plugin `plugins/Auth.kt` — installs Ktor's `Authentication` feature with a `jwt("auth-jwt")` provider:
- Validates the access token's signature and expiry using a signing secret from `.env` (alongside `DATABASE_URL`)
- Exposes `principal.payload.subject` (= `users.id`) to handlers via `call.principal<JWTPrincipal>()`
- Protected routes wrap their block in `authenticate("auth-jwt") { ... }`

New service-layer pieces (still in `app` for now — hexagonal layers come later, per the backend skeleton spec):
- `GoogleTokenVerifier` — JWKS fetch/cache + signature check against Google's published keys
- `TokenService` — mints access/refresh JWT pairs, hashes/stores/rotates refresh tokens
- `UserRepository` / `RunRepository` (Exposed-backed) — find-or-create user, query runs scoped to `user_id`

This shape mirrors the existing `routes/` + `plugins/` convention, isolates Google-specific logic in one verifier class (easy to extend to other providers later), and ensures every data-touching route is scoped by the JWT's `sub` — a user can only ever see their own runs, enforced at the query level, not just the route level.

---

## 4. Token lifecycle & error handling

**Lifetimes:**
- Access JWT: short-lived (e.g. 15 minutes) — limits the damage window if one leaks, since it can't be revoked
- Refresh token: long-lived (e.g. 30 days), stored hashed in `refresh_tokens`, revocable

**Rotation on refresh** (standard defense against stolen refresh tokens):
- Each `POST /auth/refresh` validates the presented token, **revokes it**, and issues a brand-new access+refresh pair
- If a *revoked* refresh token is ever presented again — a replay signal — the backend revokes the *entire* token family for that user, forcing a fresh Google sign-in everywhere

**Error responses** (consistent JSON shape, e.g. `{ "error": "invalid_token" }`):

| Scenario | Status | Response |
|---|---|---|
| Google ID token fails verification (bad signature/expired/wrong audience) | `401` | `invalid_id_token` |
| Access token missing/expired/malformed on a protected route | `401` | `unauthorized` (Ktor JWT plugin's `challenge` block) |
| Refresh token expired, revoked, or unknown | `401` | `invalid_refresh_token` — re-trigger Google sign-in |
| Reused (already-revoked) refresh token detected | `401` + revoke family | `refresh_token_reuse_detected` |
| Authenticated user requests a run they don't own | `404` (not `403`) | avoids leaking whether the resource exists |

On the Android side: an HTTP client interceptor catches `401`s, attempts one silent `/auth/refresh`, and retries the original request — falling back to the sign-in screen only if refresh itself fails.

---

## 5. Testing approach

Following the project's TDD workflow and the existing `HealthRouteTest` pattern (`ktor-server-test-host` + JUnit 5):

**Route/integration tests** (`app/src/test/kotlin/com/runner/routes/`)
- `AuthRouteTest` — `/auth/google` with a valid mocked Google ID token creates a user and returns a token pair; an invalid/expired token returns `401 invalid_id_token`; a second sign-in with the same `google_sub` reuses the existing user (no duplicate row)
- `AuthRefreshTest` — valid refresh returns a new pair and revokes the old one; replaying a revoked token returns `401` and revokes the whole family; expired/unknown tokens return `401 invalid_refresh_token`
- `RunsRouteTest` — `GET /runs` and `GET /runs/{id}` require a valid `Authorization` header (`401` without one); a user can't fetch another user's run (`404`); listing only returns the caller's own runs

**Unit tests** (`app/src/test/kotlin/com/runner/`)
- `GoogleTokenVerifierTest` — signature/audience/expiry validation against fixture JWKS + tokens (no live network calls to Google in tests)
- `TokenServiceTest` — JWT minting/parsing, refresh-token hashing, rotation and family-revocation logic in isolation

**Test database:** since Exposed + Postgres is the real target (no Supabase-specific code, no ORMs), tests run against a real Postgres instance — most likely via Testcontainers, matching "test what you ship." The exact mechanism should be confirmed as a concrete choice in the implementation plan, since it affects CI setup.

We deliberately don't unit-test Ktor's JWT plugin internals, Google's JWKS endpoint format, or Postgres itself — we trust the framework/library guarantees and test our integration points instead.

---

## Out of scope

- Additional social providers (GitHub, Apple, etc.) or email+password login — the data model (`google_sub` as identity anchor) leaves room to add these later via an `identities` table
- Migrating to a self-hosted identity server (Keycloak/Ory)
- Hexagonal architecture refactor of auth/run code (per the backend skeleton spec, `domain`/`infra` remain empty for now)
- Android-side implementation details (Credential Manager integration, token storage, retry interceptor) — covered at a high level here, detailed in the implementation plan
