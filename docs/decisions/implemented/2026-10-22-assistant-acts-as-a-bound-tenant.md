# Decision: The assistant acts as a tenant bound to one establishment

Status: implemented

## Problem

The assistant books, reschedules and cancels through the same booking services as staff, and those services check the caller with `AccessPolicy`. A turn runs in a job, with no signed-in member and no request. Booking must still refuse anything outside the assistant's establishment, however the model was steered (A4).

## Decision

A turn opens its own request context and calls `TenantContext.bindAssistant(organization, establishment)` with the turn's conversation. The assistant is no member and has no role: reading its user, member or role throws, so code that needs a person fails closed. `AccessPolicy.require` allows it only the actions where `Action.allowsAssistant()` is true (booking appointments), and `requireEstablishment` only its own establishment. Booking's command and query services run unchanged for staff and the assistant; the facade marks the assistant's writes with `source=assistant`, which turns on B9 and B14.

## Alternatives considered

**A facade that skips the access checks.** Booking would trust whatever ids the assistant passes. A bug in the assistant's scope would then reach another establishment's calendar with nothing to stop it.

**Access checks chosen by the command's source field.** Simpler, but permission would hang on a data field that a future endpoint could set by mistake, and the establishment check would still be missing.

**A fourth role for the assistant.** Roles describe people in the team (§5 of the product docs). An assistant role would show up in membership code and screens that have nothing to do with it.

## Consequences

The assistant gets the same 403 and 404 behavior as staff of one establishment, and booking needs no second path. Every turn needs its own request context: the scheduler may already have opened one for the job, so the turn steps out of it and back. Services that read the caller's member or user cannot be called by the assistant; that is on purpose.
