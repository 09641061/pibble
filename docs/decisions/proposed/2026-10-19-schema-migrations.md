# Decision: Schema migrations with Flyway

Status: proposed

## Problem

In dev and test, Hibernate recreates the schema on every start (`drop-and-create`). In prod the strategy is `none`, so there is no versioned way to create or change tables in a database with data. Several invariants (exclusion constraints, partial unique indexes, `CHECK`s) cannot be expressed by Hibernate's generated schema at all.

## Proposal

Add `quarkus-flyway` with `quarkus.flyway.migrate-at-start=true` and plain SQL scripts in `src/main/resources/db/migration`: one file per context when the context is created, then one file per later change. The version is the UTC creation time (`V202610211430__identity.sql`), so two branches never take the same number; a branch whose migration is older than one merged meanwhile renames it before merging. The product docs' `schema.sql` is the starting point for each context's first file. Hibernate switches to `validate` in every profile, so a mismatch between entities and schema fails at startup.

Schema conventions: `uuid` primary keys; `timestamptz` for instants and `time` for local opening hours; money as integer cents plus an ISO currency; enumerations as `text` with a `CHECK`; `created_at` and `updated_at` on every table (the columns of `AuditableAbstractPersistenceEntity`); soft delete only where history must survive (`customers.deleted_at`, `members.removed_at`, `establishments.archived_at`).

## Alternatives considered

**Hibernate `update` in prod.** It does not drop columns, does not migrate data, cannot create exclusion constraints and applies changes without review.

**Sequential versions (`V1`, `V2`, …).** Simple to read, but two parallel branches both add `V7` and the second merge breaks startup.

**Liquibase.** It works as well, but XML or YAML changelogs are more verbose than plain SQL for a team that already writes SQL.

## Acceptance criteria

- An empty PostgreSQL database gets the full schema at startup in prod.
- Every profile uses `validate` and starts without errors on that database.
- A test with Dev Services applies the migrations and passes, including one that hits an exclusion constraint.
