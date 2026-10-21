# Decision: The database enforces invariants that concurrency can break

Status: implemented

## Problem

Staff and the assistant book at the same time. A read-then-write check in Java lets two requests both pass, which is how the legacy could double-book a staff member.

## Decision

Invariants that must never break under concurrency are database constraints: no overlapping `confirmed` or `in_progress` appointments per staff member or per customer (`appointments_staff_no_overlap`, `appointments_customer_no_overlap`, `EXCLUDE USING gist`), one active customer per phone (`customers_phone_uk`), exactly one owner per organization (`members_one_owner_uk`), and one effect per idempotency key. The tables of later contexts follow the same rule; the [takodu-migrations](../../../.agents/skills/takodu-migrations/SKILL.md) skill lists each constraint and the exception it becomes.

The domain checks the same rules first (`AvailabilityPolicy`, `Member`, `Customer`) to return a clear error. The repository adapter flushes on save and translates the constraint it owns, by name, into the context's exception, such as `SlotUnavailableException` (409 `SLOT_UNAVAILABLE`). Hibernate does not name exclusion constraints, so `AppointmentRepositoryImpl` reads the name from the PostgreSQL error.

Rules that span tables (inside opening hours, staff performs the service, no time block) are checked in the domain inside one transaction; the plan's establishment limit is checked under `SELECT … FOR UPDATE` on the organization.

## Alternatives considered

**Application checks only.** Simpler migrations, but no guarantee under concurrent requests.

**Serializable transactions.** They would catch the conflict, but retries spread through every service and the error no longer names the rule.

## Consequences

Six simultaneous bookings of one member and slot end with one appointment and five 409 responses (`AppointmentsControllerTest`), and an overlapping write that skips `AvailabilityPolicy` still fails with `SlotUnavailableException` (`AppointmentRepositoryImplTest`). An unmapped violation stays a 500 on purpose, because it means a rule was missed. A rule that cannot be a constraint, such as opening hours, can still race with a concurrent change of the hours; booked appointments outside new hours are kept by design (O8).
