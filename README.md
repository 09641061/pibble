# pibble

Backend of Takodu: a Quarkus REST API (Java 25, PostgreSQL) organized as a modular monolith with DDD. Small service businesses use it to take bookings and answer customers on WhatsApp, with an AI assistant and a shared inbox. The architecture is in [docs/architecture.md](docs/architecture.md); instructions for agents and contributors are in [AGENTS.md](AGENTS.md).

## Requirements

- [mise](https://mise.jdx.dev/) for the JDK: `mise install` installs the version pinned in `mise.toml`.
- Docker, so that Dev Services starts PostgreSQL in dev and in tests.

## Run

```sh
./mvnw quarkus:dev
```

- API: `http://localhost:8080/api/v1/...`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Dev UI: `http://localhost:8080/q/dev-ui`
- Health: `/q/health/live` and `/q/health/ready` (database and media volume), without a token ([decision](docs/decisions/implemented/2026-10-23-health-checks-cover-only-the-process.md)).
- Sign in locally: `POST /api/v1/auth/login-codes` with any email; dev mode prints the code to the console instead of emailing it.
- Invite a teammate locally: `POST /api/v1/members`; dev mode prints the invitation token, which the invitee sends to `POST /api/v1/invitation-acceptances` after signing in with the invited email.

## Tests

```sh
./mvnw test                        # all
./mvnw test -Dtest=ArchitectureTest  # layer, context and context-map rules
scripts/check-docs.sh              # documentation
```

## Production

The `prod` profile reads its settings from environment variables, which [.env.example](.env.example) lists; Quarkus and Docker Compose load them from a `.env` file at the root, never committed. **The application refuses to start when a secret is missing** (`ProductionConfigTest` lists them). Production logs JSON, serves neither Swagger UI nor the OpenAPI document, and accepts browser calls only from `APP_URL`.

Pibble runs as one JVM container (`src/main/docker/Dockerfile.jvm`, uid 185) next to PostgreSQL on one VPS ([decision](docs/decisions/implemented/2026-10-19-quarkus-on-the-jvm.md)). Run **one app instance**: jobs are safe on several, but the inbox stream and the media volume are not ([decision](docs/decisions/implemented/2026-10-19-postgresql-as-the-only-infrastructure.md)).

### Deploy

```sh
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/privateKey.pem
openssl rsa -in keys/privateKey.pem -pubout -out keys/publicKey.pem
sudo chown 185 keys/*.pem                 # the app user reads them
openssl rand -base64 32                   # value for TOKEN_ENCRYPTION_KEY in .env
./mvnw package -DskipTests && docker compose up -d --build
```

[docker-compose.yml](docker-compose.yml) runs the image with PostgreSQL 18, the `media` volume at `/deployments/media` and the keys from `keys/`. To deploy a new version, pull and run the last line again; Compose waits 30 seconds on stop, longer than `quarkus.shutdown.timeout`.

### Migrations

Flyway applies pending migrations at startup, before the application serves requests; a failed migration stops the startup and the previous container keeps the old schema. Migrations only move forward: fix a released one with a new migration ([takodu-migrations](.agents/skills/takodu-migrations/SKILL.md)).

### First platform operator

The person signs in once with their email, then in `docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`:

```sql
UPDATE users SET platform_operator = true WHERE email = 'ana@takodu.com';
```

Set it back to `false` to remove access ([decision](docs/decisions/implemented/2026-10-23-takodu-team-as-platform-operators.md)).

### Backups

```sh
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > pibble-$(date +%F).dump
docker compose exec -T app tar cf - -C /deployments/media . > media-$(date +%F).tar
```

Dump the database first, so every message in the dump has its file. Restore with `pg_restore --clean` into the same database and `docker compose exec -T app tar xf - -C /deployments/media < media-….tar`. Keep `.env` in a separate safe place: without `TOKEN_ENCRYPTION_KEY` the stored WhatsApp tokens cannot be read.

### Rotating TOKEN_ENCRYPTION_KEY

The key encrypts only the WhatsApp business tokens in `whatsapp_lines`, and Pibble holds one key at a time. After a new key is set and the app restarted, each line fails to send and download media until its owner connects it again through Embedded Signup. Rotate only when the key may have leaked, and tell the owners first.

### Health

`/q/health/live` answers while the process serves HTTP; `/q/health/ready` also checks the database and the media volume. Neither needs a token, and neither calls Meta, the LLM or Resend ([decision](docs/decisions/implemented/2026-10-23-health-checks-cover-only-the-process.md)).

## Local hooks

```sh
git config core.hooksPath .githooks
```

`pre-commit` checks whitespace and documentation, `commit-msg` checks the commit message, and `pre-push` runs `ArchitectureTest`.

## Roadmap

Each step is one pull request into `develop`.

| Step | Scope | PR |
|---|---|---|
| 0 | Shared kernel, errors, tenancy, architecture rules | [#1](https://github.com/09641061/pibble-api/pull/1) |
| 1 | `iam`: passwordless sign-in and sessions | [#2](https://github.com/09641061/pibble-api/pull/2) |
| 2 | `organization`: establishments, members, services | [#3](https://github.com/09641061/pibble-api/pull/3) |
| 3 | `customers` | [#4](https://github.com/09641061/pibble-api/pull/4) |
| 4 | `booking`: appointments, time blocks, availability | [#5](https://github.com/09641061/pibble-api/pull/5) |
| 5a | `conversations`: WhatsApp lines and the inbox | [#6](https://github.com/09641061/pibble-api/pull/6) |
| 5b | Appointment notifications | [#7](https://github.com/09641061/pibble-api/pull/7) |
| 6a | `assistant` | [#8](https://github.com/09641061/pibble-api/pull/8) |
| 6b | Interactive replies and pause | [#9](https://github.com/09641061/pibble-api/pull/9) |
| 7 | Inbox media and templates | [#10](https://github.com/09641061/pibble-api/pull/10) |
| 8a | Takodu team: plans, usage, media retention | [#11](https://github.com/09641061/pibble-api/pull/11), [#13](https://github.com/09641061/pibble-api/pull/13) |
| 8b | Release readiness: health, production profile, container, CI | [#15](https://github.com/09641061/pibble-api/pull/15) |

Next: the first release, `develop` into `main`.

Backlog:

- Inbox: last-message preview and handoff notes.
- Customer search that ignores accents (`unaccent`).
- Reminder to the Takodu team before a plan expires.
- No duplicate when staff and a scheduled reminder notify the same appointment.
- A previous `TOKEN_ENCRYPTION_KEY` accepted while tokens are re-encrypted, so rotation needs no reconnection.
- Several app instances: shared media storage and `LISTEN/NOTIFY` behind the inbox stream.
