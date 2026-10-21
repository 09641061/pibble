# Decision: Manual plan payment

Status: proposed

## Problem

About twenty businesses pay by bank transfer, Yape or Plin. The legacy required a Stripe subscription before an organization could even be created, tied the subscription to the owner's user instead of the organization, and defined prices in two places.

## Proposal

The plan lives on the organization: `plan` (`standard` with one active establishment, `max` with any number) and `plan_active_until`. Organizations are created freely; the Takodu team sets `plan_active_until` after receiving payment, including the trial. The plan enum in the `organization` domain holds the establishment limits; prices stay outside the system. While the plan is inactive, reads and inbound storage work, and writes that act on customers (booking, sending, assistant replies, reminders) throw `PlanInactiveException`.

## Alternatives considered

**Stripe subscriptions.** Automates renewals, but adds secrets, webhooks and a boot dependency for clients who do not pay by card.

**No plan state at all.** Simplest, but the establishment limit and the trial would have no home.

## Acceptance criteria

- Creating an organization needs no payment data.
- With `plan_active_until` in the past, booking returns 402 with code `PLAN_INACTIVE` and inbound messages are still stored.
