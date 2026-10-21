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
- Sign in locally: `POST /api/v1/auth/login-codes` with any email; dev mode prints the code to the console instead of emailing it.

## Tests

```sh
./mvnw test                        # all
./mvnw test -Dtest=ArchitectureTest  # layer, context and context-map rules
scripts/check-docs.sh              # documentation
```

## Production

The `prod` profile reads the database, Resend and JWT key settings from environment variables; [.env.example](.env.example) lists them. Quarkus loads a `.env` file at the root if it exists; never commit it.

```sh
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Deployment is one JVM container (`src/main/docker/Dockerfile.jvm`) next to PostgreSQL on one VPS ([decision](docs/decisions/implemented/2026-10-19-quarkus-on-the-jvm.md)).

## Local hooks

```sh
git config core.hooksPath .githooks
```

`pre-commit` checks whitespace and documentation, `commit-msg` checks the commit message, and `pre-push` runs `ArchitectureTest`.
