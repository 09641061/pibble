# Decision: Idempotency keys on retried requests

Status: proposed

## Problem

Staff use Takodu from phones on weak salon wifi and do not always remember what they already did. When a response is slow or lost, they tap again. Without protection, a second tap sends the same WhatsApp message twice, and a second booking tap fails with a confusing 409 although the first one worked. The assistant has the same problem when a failed turn is retried.

## Proposal

Every POST that a person can retry (send a message or template, book, reschedule, cancel, change appointment status, register a customer, create a time block, pause or resume the assistant) requires an `Idempotency-Key` header: a UUID that the client generates once per user action and resends unchanged on every retry. A missing or malformed header is 400. The controller passes it into the command as `shared.domain.model.valueobjects.IdempotencyKey`. A POST whose repeat is harmless or deduplicated another way (the Meta webhook, requesting or verifying a sign-in code) is annotated `@WithoutIdempotencyKey` with the reason; `ArchitectureTest` fails any other POST without the header parameter.

- **Creations** store the key on the created row (`idempotency_key uuid`, unique index `<table>_idempotency_uk`). The command service first looks the key up within the organization; if found, it returns that aggregate and the controller answers exactly as the first time. A concurrent duplicate hits the unique index; PostgreSQL aborts that transaction, so the adapter throws `RequestInProgressException` (a `ConflictException`, code `REQUEST_CONFLICT`) and the client's next retry with the same key returns the first result.
- **Transitions** store the last applied key on the aggregate (`last_idempotency_key`). A command whose key equals it returns the current state without applying the change again, so a second cancel tap returns the cancelled appointment instead of a 422.
- **The assistant** derives the key of each write from the turn: `UUID.nameUUIDFromBytes(turnId + toolName + arguments)`, so a retried turn that repeats a booking gets the existing appointment.
- A key reused with a different body returns the first result; the client must create a new key for each new action.

Webhooks and jobs keep their own keys (`wamid`, one row per appointment and kind, one turn per message).

## Alternatives considered

**A generic idempotency table with stored responses, filled by a request filter.** It covers every endpoint without touching aggregates, but storing and replaying HTTP bodies across the transaction boundary adds a second source of truth and fails awkwardly when the first request is still running.

**Rely on natural uniqueness only** (phone per organization, no overlapping appointments). It prevents some duplicates, but turns a successful retry into an error, and messages have no natural key at all.

**Disable the button on the client.** Helps, but a lost response still leaves the person unsure, and only the server can guarantee one effect per action.

## Acceptance criteria

- Sending the same message request twice with one key stores one message and sends it once; both responses carry the same message id.
- Booking twice with one key returns the same appointment both times, with no 409.
- Cancelling twice with one key returns the cancelled appointment both times.
- A retried assistant turn that books again creates no second appointment.
