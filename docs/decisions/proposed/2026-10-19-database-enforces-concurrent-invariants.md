# Decision: The database enforces invariants that concurrency can break

Status: proposed

## Problem

Staff and the assistant book at the same time. A read-then-write check in Java lets two requests both pass, which is how the legacy could double-book a staff member.

## Proposal

Invariants that must never break under concurrency are database constraints: no overlapping `confirmed` or `in_progress` appointments per staff member or per customer (`EXCLUDE USING gist`), exactly one owner per organization, one WhatsApp line per establishment and one establishment per number, one message per `wamid`, one assistant turn per inbound message. The domain checks the same rules first (`AvailabilityPolicy`, `Member`, `TurnEnqueuer`) to return a clear error. The repository adapter flushes on save and translates the SQL state (`23P01`, `23505`) of the constraint it owns into the context's exception, such as `SlotUnavailableException`.

Rules that span tables (inside opening hours, staff performs the service, no time block) are checked in the domain inside one transaction; the plan's establishment limit is checked under `SELECT … FOR UPDATE` on the organization.

## Alternatives considered

**Application checks only.** Simpler migrations, but no guarantee under concurrent requests.

**Serializable transactions.** They would catch the conflict, but retries spread through every service and the error no longer names the rule.

## Acceptance criteria

- A test that books the same staff member and slot from two concurrent transactions ends with exactly one appointment and one 409.
- Each constraint violation reaches the client as a context exception code, never as 500.
