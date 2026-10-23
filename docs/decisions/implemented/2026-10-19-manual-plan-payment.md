# Decision: Manual plan payment

Status: implemented

## Problem

About twenty businesses pay by bank transfer, Yape or Plin. The legacy required a Stripe subscription before an organization could even be created, tied the subscription to the owner's user instead of the organization, and defined prices in two places.

## Decision

The plan lives on the organization: `plan` (`standard` with one active establishment, `max` with any number) and `plan_active_until`. Organizations are created freely on a trial (`takodu.organization.trial-days`). The plan enum in the `organization` domain holds the establishment limits; prices stay outside the system, and no code computes money.

After a payment, a [platform operator](2026-10-23-takodu-team-as-platform-operators.md) calls `POST /internal/v1/organizations/{id}/plan-changes` with an `Idempotency-Key`, the plan, either `activeUntil` or `extendDays`, and a note. Extending counts from the current end while the plan is active and from now once it has ended. The organization row lock serializes changes, so a repeated key returns the first change. Each change appends a `plan_changes` row (who, when, from, to, note), which a trigger keeps append-only; `GET` on the same path lists them for a dispute, and `GET /internal/v1/organizations` shows every plan, owner email and establishment count. A downgrade to `standard` keeps open establishments open and blocks new ones until the count is below the limit (O4).

While the plan is inactive, reads and inbound storage work, and writes that act on customers (booking, sending, assistant replies, reminders) throw `PlanInactiveException`.

## Alternatives considered

**Stripe subscriptions.** Automates renewals, but adds secrets, webhooks and a boot dependency for clients who do not pay by card.

**No plan state at all.** Simplest, but the establishment limit and the trial would have no home.

**Editing `plan_active_until` in the database.** No endpoint to build, but no history to answer "I paid in March", no idempotency when a script is run twice, and no protection against a typo in production.

**Archiving establishments above the limit on a downgrade.** Enforces the plan strictly, but would close a branch with booked appointments because a payment was late; the business instead archives the one it chooses.

## Consequences

Creating an organization needs no payment data, and renewals are a person's job: a forgotten payment ends the plan, and the team watches `planActive` and `planActiveUntil` in the organization list. With `plan_active_until` in the past, booking returns 402 with code `PLAN_INACTIVE` and inbound messages are still stored. The history is evidence only if operators write honest notes; it cannot be edited afterwards, so a mistake is corrected with a new change.
