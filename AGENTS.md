# AGENTS.md

Pibble is the Takodu backend: a Quarkus REST API organized as a modular monolith with DDD. Takodu lets small service businesses in Peru take bookings and answer customers on WhatsApp, with an AI assistant and a shared inbox. Read [docs/architecture.md](docs/architecture.md) before changing `src/main/java`, and follow [docs/AGENTS.md](docs/AGENTS.md) for documentation.

## Product specification

The business rules live outside this repository, in the Takodu product docs (`../docs` in the workspace): `03-business-domain.md` (rules with ids such as `B3` or `W6`), `05-domain-model.md`, `06-database-schema.md` with `schema.sql`, `07-whatsapp-integration.md` and `08-ai-assistant.md`. Read the sections of the context you touch before writing code. **On business rules the product docs win; on code shape (packages, names, error types, events) this repository wins.**

## Required skills

Skills live in `.agents/skills/` (`.claude/skills` is a symlink to that folder). Load them before acting:

- Any change to Java code: [takodu-ddd](.agents/skills/takodu-ddd/SKILL.md), which defines contexts, layers, packages and names.
- New capability or endpoint: [takodu-feature](.agents/skills/takodu-feature/SKILL.md).
- Fixing a defect: [takodu-fix](.agents/skills/takodu-fix/SKILL.md).
- Code in one context that needs another context: [takodu-context-integration](.agents/skills/takodu-context-integration/SKILL.md).
- Documentation, decisions or commit messages: [takodu-docs](.agents/skills/takodu-docs/SKILL.md).
- Endpoints, permissions, repository scoping, or anything that needs the caller's organization: [takodu-tenancy](.agents/skills/takodu-tenancy/SKILL.md).
- Tables, columns, indexes, constraints or Flyway: [takodu-migrations](.agents/skills/takodu-migrations/SKILL.md).
- Scheduled or deferred work, or any call to Meta, the LLM or Resend: [takodu-jobs](.agents/skills/takodu-jobs/SKILL.md).
- Receiving or sending WhatsApp messages: [takodu-whatsapp](.agents/skills/takodu-whatsapp/SKILL.md).
- The AI assistant, its tools, prompt or model: [takodu-assistant](.agents/skills/takodu-assistant/SKILL.md).

## Commands

```sh
mise install                 # JDK 25 (pinned in mise.toml)
./mvnw quarkus:dev           # dev mode; Dev Services starts PostgreSQL in Docker
./mvnw test                  # unit, architecture and endpoint tests
./mvnw test -Dtest=ArchitectureTest  # layer, context and context-map rules only
scripts/check-docs.sh        # word budgets, links, decision records and skills
git config core.hooksPath .githooks  # enables the local hooks (once per clone)
```

Before pushing, run the evidence that matches the change: tests of the touched domain or endpoint, `ArchitectureTest` if you moved or created classes, and `scripts/check-docs.sh` if you touched Markdown. Report only the commands you ran.

## Permanent rules

- **Dependencies point inward:** `interfaces → application → domain ← infrastructure`. The domain is plain Java, without Quarkus, JPA, REST or LangChain4j. `ArchitectureTest` enforces it; fix the placement, never weaken the rule.
- **Contexts meet only through `interfaces.acl` (facades) and `interfaces.events` (integration events)**, and only along the context map in [docs/architecture.md](docs/architecture.md). Never import another context's domain, application or persistence.
- **Expected failures are typed domain exceptions** extending a base in `shared.domain.exceptions`; `GlobalExceptionMapper` is the only place that chooses the HTTP status ([decision](docs/decisions/implemented/2026-10-19-errors-with-exception-mapper.md)).
- **Aggregates have no setters.** Every state change is a method with a business name that checks its invariant.
- **Tenant, role and owner never come from a request body.** They come from `TenantContext`, and every repository query is scoped by `OrganizationId` ([decision](docs/decisions/proposed/2026-10-19-tenancy-and-fixed-roles.md)).
- **Identifiers are UUIDs** wrapped in typed value objects (`CustomerId`); money is integer cents plus currency; instants are UTC `Instant`.
- **Documentation travels with the code** in the same commit. A decision with real alternatives is recorded in `docs/decisions/`.
- **Commits:** a Conventional Commits subject with the context as scope (`feat(booking): …`), no body or one short line, no attribution trailers; `scripts/check-commit-msg.sh` enforces it. Reasons and evidence go in the pull request's Proof block.
- **Branches:** `<type>/<topic>`, or `<agent>/<type>-<topic>` for agents; never `--force` ([takodu-docs](.agents/skills/takodu-docs/SKILL.md)).
- **Secrets:** never commit `.env`; document new variables in `.env.example`.
- **Language:** everything in the repository is in English: instructions, docs, decision records, skills, code, Javadoc and commits.

## Editing these instructions

`CLAUDE.md` is a symlink to this file; edit `AGENTS.md`. Each rule takes one to three lines and links to where it is explained. The word budget is in [scripts/doc-budgets.tsv](scripts/doc-budgets.tsv).
