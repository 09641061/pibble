---
name: takodu-migrations
description: How to create and change the PostgreSQL schema of the pibble backend — Flyway migrations derived from the product schema.sql, naming and conventions, constraints that guard invariants (exclusion, partial unique, CHECK), translating their SQL state into domain exceptions in the repository adapter, and concurrency tests with Dev Services. Use when a context gets its first tables, when adding or changing a column, index or constraint, or when a duplicate or overlap reaches the client as 500.
---

# Schema and migrations in pibble

The schema is versioned SQL, and the database guards every invariant that concurrent requests can break. The decisions are [schema migrations](../../../docs/decisions/implemented/2026-10-19-schema-migrations.md) and [the database enforces concurrent invariants](../../../docs/decisions/proposed/2026-10-19-database-enforces-concurrent-invariants.md). The target tables are in the product docs (`../docs/schema.sql`, explained in `../docs/06-database-schema.md`).

## Setup

Flyway runs the migrations at startup in every profile (`quarkus.flyway.migrate-at-start=true`). `SchemaMigrationsTest` checks that nothing is pending and no checksum changed.

## Files

- Location: `src/main/resources/db/migration`.
- The version is the UTC creation time, `V<yyyyMMddHHmm>__<description>.sql`, so parallel branches never collide. A context's first file creates all its tables (`V202610211430__identity.sql`); every later change is a new file (`V202611031015__booking_add_no_show_reason.sql`). Build contexts in dependency order (identity first, assistant last).
- If `main` gained a newer migration meanwhile, rename yours to a new timestamp before merging; Flyway refuses a version older than an applied one.
- **Never edit or rename a migration after it is on `main`**; Flyway's checksum fails on every database that ran it.
- Copy the context's tables from `../docs/schema.sql`, then apply the conventions below where it differs.

## Conventions

- `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, plus `created_at timestamptz NOT NULL DEFAULT now()` and `updated_at timestamptz NOT NULL DEFAULT now()` on every table mapped by an entity extending `AuditableAbstractPersistenceEntity`.
- Instants are `timestamptz`; establishment-local times (`opening_hours`) are `time`.
- Money is `integer` cents plus `char(3)` currency.
- Enumerations are lower-case `text` with a `CHECK (x IN (...))`; the converter maps them to the Java enum.
- Soft delete only on `customers.deleted_at`, `members.removed_at` and `establishments.archived_at`; uniqueness on those tables is a partial index `WHERE <column> IS NULL`.
- Foreign keys across contexts are allowed; name every constraint and index explicitly (`<table>_<what>_uk`, `<table>_<what>_idx`, `<table>_<what>_no_overlap`), because adapters match on the name.
- Tables created by retried requests get `idempotency_key uuid` with a unique index `<table>_idempotency_uk`; aggregates changed by retried transitions get `last_idempotency_key uuid`.
- `CREATE EXTENSION IF NOT EXISTS btree_gist;` goes in the first migration that uses an exclusion constraint (booking's first file).

## Constraints that guard invariants

| Invariant | Constraint | Adapter throws |
|---|---|---|
| B5, B6 no overlapping active appointments | `appointments_staff_no_overlap`, `appointments_customer_no_overlap` (`EXCLUDE USING gist`) | `SlotUnavailableException` (409) |
| C1 one active customer per phone | `customers_phone_uk` (partial unique) | `CustomerAlreadyRegisteredException` (409) |
| O3 exactly one owner | `members_one_owner_uk` | `OwnerAlreadyExistsException` (409) |
| W1 one line per establishment and number | unique `establishment_id`, `phone_number_id` | `LineAlreadyConnectedException` (409) |
| One effect per user action | `<table>_idempotency_uk` | `RequestInProgressException` (409); the retry returns the first result |
| W4 one message per `wamid` | unique `wamid` | none: the webhook treats it as a duplicate and skips |
| One assistant turn per message | unique `trigger_message_id` | none: the handler skips |

The domain checks the same rule first to give a precise error; the constraint is the guarantee under concurrency. The adapter flushes inside `save` so the violation surfaces there, not at commit:

```java
} catch (ConstraintViolationException e) {           // org.hibernate.exception
    switch (e.getConstraintName()) {
        case "appointments_staff_no_overlap", "appointments_customer_no_overlap" -> throw new SlotUnavailableException("taken");
        case null, default -> throw e;
    }
}
```

Match on the constraint name, not only the SQL state (`23P01` exclusion, `23505` unique), so two constraints on one table map to different errors. An unmapped violation stays a 500 on purpose: it means a rule was missed.

## Entities must match the migration

Hibernate runs with `validate`, so an entity column without a migration fails at startup. In the same commit, the migration adds the column and the persistence entity, converter and assembler map it ([takodu-ddd](../takodu-ddd/SKILL.md)). Use `@Version` with an `integer version` column on aggregates that staff and jobs change concurrently (`appointments`, `conversations`).

## Tests

- `@QuarkusTest` runs against Dev Services PostgreSQL with the real migrations, so a migration error fails the suite.
- For every constraint in the table above, write one test that violates it through the service and asserts the domain exception or the 409 `code`.
- Concurrency: start two transactions on separate threads that both pass the domain check (book the same member and slot), then assert exactly one row and one `SlotUnavailableException`. Use a `CountDownLatch` so both read before either writes.

## Checklist

- [ ] The change is a new `V<yyyyMMddHHmm>__…sql` file, newer than every migration on `main`; none on `main` was edited.
- [ ] Constraints and indexes have explicit names that the adapter uses.
- [ ] The entity, converter and assembler match the migration in the same commit.
- [ ] Each guarding constraint has a test, and concurrency-sensitive ones have a concurrent test.
