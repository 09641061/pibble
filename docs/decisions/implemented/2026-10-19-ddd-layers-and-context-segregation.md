# Decision: DDD layers and context segregation

Status: implemented

## Problem

The backend has six bounded contexts edited by different people and agents. Without mechanical rules, business logic ends up in controllers, JPA entities become the domain model and contexts read each other's tables, which is how the legacy Takodu backend lost its booking rules behind stubbed adapters.

## Decision

Pibble is one deployable modular monolith. Each context is a subpackage of `com.takodu.api` with four layers (`domain`, `application`, `infrastructure`, `interfaces`) and dependencies point inward. The domain is plain Java; persistence uses JPA entities separate from the aggregates, with assemblers between them. Writes go through command services and reads through query services, with `handle(...)` methods. Contexts communicate only through ACL facades and integration events, along the context map in [architecture.md](../../architecture.md).

`ArchitectureTest` enforces domain purity, layer direction, context crossings, the context map, the absence of cycles and the name suffixes. The [takodu-ddd](../../../.agents/skills/takodu-ddd/SKILL.md) skill holds the full rules and templates.

The conventions take the segregation of learning-center-platform (separate persistence, ACL with facade, separate integration events, explicitly named assemblers) and the Quarkus improvements of fudi (Panache, normalizing value objects, ArchUnit). Where the two projects differed, one form was chosen: `handle(...)` in every service, aggregates without setters with a rehydration constructor, explicit `@Convert` converters, and facades that return `Optional` instead of `0L`.

## Alternatives considered

**Panache entities as the domain model (active record).** Less code, but it mixes JPA annotations with invariants, prevents testing the domain without a database and couples every rule to the schema.

**Packages by technical layer at the root** (`controllers/`, `services/`, `repositories/`). Simple at first, but it expresses no business boundary and makes it impossible to verify which context depends on which.

**Microservices per context.** The network would guarantee the boundaries, at the cost of deployments, distributed transactions and hosting that about twenty businesses on one VPS do not justify.

## Consequences

Each feature creates more files (command, resource, two assemblers, persistence entity, adapter), and the skills exist to make that cost mechanical. In exchange, the domain is tested with plain JUnit, a schema change touches only `infrastructure`, and an agent that puts a class in the wrong place gets a failing test instead of a late review.
