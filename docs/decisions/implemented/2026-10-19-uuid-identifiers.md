# Decision: UUID identifiers

Status: implemented

## Problem

Identifiers appear in URLs, in WhatsApp webhook correlation, in integration events and across six contexts. Sequential numbers reveal how many businesses and appointments exist, and a bare `Long` passed between contexts is easy to swap for the wrong id.

## Decision

`AuditableAbstractPersistenceEntity` declares a `UUID id` generated on insert (`GenerationType.UUID`). Aggregates carry typed identifier value objects (`CustomerId`, `AppointmentId`); the identifiers that several contexts use (`OrganizationId`, `EstablishmentId`, `MemberId`, `CustomerId`, `UserId`, `ServiceId`) live in `shared.domain.model.valueobjects`. Facades, views and integration events carry plain `UUID`.

## Alternatives considered

**Sequential `bigint` keys.** Smaller indexes and readable ids, but they leak volume, invite enumeration in URLs and cannot be generated outside the database.

**Bare `UUID` in the domain.** Fewer classes, but a method taking two `UUID` parameters accepts them in the wrong order without complaint.

## Consequences

Every aggregate needs a small identifier record and every converter maps it to `uuid`. In exchange, ids are safe in URLs and events, and the compiler rejects a `CustomerId` where a `MemberId` is expected.
