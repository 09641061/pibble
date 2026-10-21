# Decision: Integration events fired by command services

Status: implemented

## Problem

When a context records a fact (an appointment was booked, a customer message arrived), other contexts must react without the publisher knowing them: `conversations` schedules the reminder, `assistant` enqueues a turn. Someone has to own creating the event, firing it and choosing the type listeners see.

## Decision

The publisher defines a `<Fact>IntegrationEvent` record in `<context>.interfaces.events`, with plain types. Its command service injects `Event<<Fact>IntegrationEvent>` and calls `fire(...)` after `save` returns the aggregate with its id, inside the same transaction. Each listener is a `<Fact>EventHandler` in its own context's `application.internal.eventhandlers`, and translates the event into a command of its own. The domain neither records nor knows events. The timing rules (`@Observes` versus `AFTER_SUCCESS`) are in [takodu-context-integration](../../../.agents/skills/takodu-context-integration/SKILL.md).

## Alternatives considered

**Domain events recorded on the aggregate and published by the repository adapter, as in learning-center-platform.** It separates the internal event from the published language, but needs an aggregate base class, an extra translation step and a post-save hook to get the id. With one event type the same separation holds (the domain does not know the published record) with fewer parts.

**Direct calls to the other context's facade.** The publisher would depend on every listener, and `booking` calling `conversations` would create a cycle.

**A message broker.** It adds infrastructure and asynchronous delivery that one process does not need ([decision](../proposed/2026-10-19-postgresql-as-the-only-infrastructure.md)).

## Consequences

Business rules that depend on the fact live in the command service, not in the aggregate, and a synchronous listener that fails rolls back the publisher's operation. In exchange, there is one mechanism, easy to test with CDI, and `ArchitectureTest` verifies that only handlers import another context's events.
