# Pibble architecture

Pibble is a modular monolith: one Quarkus application and one PostgreSQL database, split into six bounded contexts whose boundaries `ArchitectureTest` verifies. The exact shape of each package and class is in the [takodu-ddd](../.agents/skills/takodu-ddd/SKILL.md) skill; why the contexts are cut this way is in the [bounded contexts decision](decisions/proposed/2026-10-19-six-bounded-contexts.md).

## Contexts

Each bounded context is a direct subpackage of `com.takodu.api`. `shared` is the shared kernel and depends on no context.

| Context | Type | Aggregates | May depend on | Publishes (events) |
|---|---|---|---|---|
| `shared` | kernel | — | — | — |
| `iam` | generic | `User`, `LoginCode`, `Session` | — | — |
| `organization` | supporting | `Organization`, `Establishment`, `Member`, `Service` | `iam` | — |
| `customers` | supporting | `Customer` | — | — |
| `booking` | core | `Appointment`, `TimeBlock` | `organization`, `customers` | `AppointmentBooked`, `AppointmentRescheduled`, `AppointmentCancelled` |
| `conversations` | core | `WhatsappLine`, `Conversation`, `Message`, `ScheduledMessage` | `organization`, `customers`, `booking` (events only) | `InboundMessageReceived` |
| `assistant` | core | `AssistantSettings`, `AssistantTurn` | `organization`, `customers`, `booking`, `conversations` | — |

`ArchitectureTest.ALLOWED_DEPENDENCIES` mirrors the "May depend on" column, and a class in a package missing from it fails the build. Change the table, the map and, for a new context, a decision record in the same commit.

The shared kernel holds the typed identifiers used by several contexts (`OrganizationId`, `EstablishmentId`, `MemberId`, `CustomerId`, `UserId`, `ServiceId`), `Money`, `TimeRange`, `IdempotencyKey`, the tenancy holder `TenantContext` with `AccessPolicy`, the domain exceptions, `GlobalExceptionMapper`, `ErrorResource`, `AuditableAbstractPersistenceEntity` and the UTC `Clock` producer.

## Layers inside a context

```text
interfaces      REST controllers, resources and assemblers; request filters; ACL facade (interface and views); integration events
application     command and query services; facade implementation; event handlers; scheduled jobs; assistant tools; outbound ports
domain          aggregates, entities, value objects, commands, queries, exceptions, domain services, repository ports
infrastructure  JPA entities, Panache repositories, persistence assemblers, adapters, converters; Meta, LLM and Resend clients
```

Dependencies go `interfaces → application → domain ← infrastructure`. The domain depends only on the JDK and Lombok.

## Communication between contexts

- **Call:** the consumer uses the supplier's facade (`<supplier>.interfaces.acl`) only from `application.internal.outboundservices`, and translates its views into its own value objects.
- **Event:** the publisher fires a `<Fact>IntegrationEvent` from `interfaces.events` after saving; the consumer observes it in `application.internal.eventhandlers`.
- **Calls go down, events go up:** a context calls only contexts it may depend on. When a lower context must trigger work in a higher one, it publishes an event. There are no cycles.

| Event | Listener | Phase | What the listener does |
|---|---|---|---|
| `AppointmentBooked`, `AppointmentRescheduled`, `AppointmentCancelled` | `conversations` | in transaction | Schedules, moves or cancels confirmation and reminder rows |
| `InboundMessageReceived` | `assistant` | in transaction | Enqueues an assistant turn when the assistant may answer |
| `InboundMessageReceived` | `assistant` | after commit | Wakes the turn worker |

In-transaction listeners write only their own tables, so a failure rolls back the whole operation. After-commit listeners only wake work up; a polling job recovers anything lost. Inside `conversations`, an after-commit `ConversationChanged` CDI event feeds the SSE inbox stream; it is not an integration event. Event names above omit the `IntegrationEvent` suffix. The detail and templates are in [takodu-context-integration](../.agents/skills/takodu-context-integration/SKILL.md).

## Requests, tenancy and background work

A request passes JWT verification, then `organization`'s `TenantFilter` loads the caller's active membership for the `X-Organization-Id` header into `TenantContext` or answers 403. Command services check `AccessPolicy` before loading the aggregate. Webhooks and scheduled jobs have no `TenantContext`; they derive the organization from the WhatsApp line or the appointment. The rules are in the [tenancy decision](decisions/proposed/2026-10-19-tenancy-and-fixed-roles.md).

Scheduled jobs use `quarkus-scheduler` and claim rows with `SELECT … FOR UPDATE SKIP LOCKED`; outgoing WhatsApp messages and assistant turns are rows in their context's tables ([decision](decisions/proposed/2026-10-19-webhooks-store-first-and-polled-outbox.md)). Every external system (Meta, the LLM provider, Resend) sits behind a port in `application.internal.outboundservices`.

## Persistence

Each table belongs to one context; foreign keys across contexts are allowed, but no context reads or writes another context's table. Primary keys are UUIDs. Invariants that concurrency can break are also database constraints, and the repository adapter translates their SQL state into a domain exception ([decision](decisions/proposed/2026-10-19-database-enforces-concurrent-invariants.md)). Migrations follow the [migrations decision](decisions/implemented/2026-10-19-schema-migrations.md).

## HTTP errors

| Exception | Status |
|---|---|
| `IllegalArgumentException` (invalid value object or command) | 400 |
| `PlanInactiveException` | 402 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `BusinessRuleViolationException` | 422 |
| Anything else | 500 (Quarkus logs it) |

The body is `{"code": "...", "message": "..."}`. Bean Validation rejections (`@Valid` on a resource) are also 400, but Quarkus returns them in its own violations format. The reason is in the [errors decision](decisions/implemented/2026-10-19-errors-with-exception-mapper.md).
