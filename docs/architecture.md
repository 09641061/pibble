# Pibble architecture

Pibble is a modular monolith: one Quarkus application and one PostgreSQL database, split into six bounded contexts whose boundaries `ArchitectureTest` verifies. The exact shape of each package and class is in the [takodu-ddd](../.agents/skills/takodu-ddd/SKILL.md) skill; why the contexts are cut this way is in the [bounded contexts decision](decisions/implemented/2026-10-19-six-bounded-contexts.md).

## Contexts

Each context is a direct subpackage of `com.takodu.api`; `shared` is the kernel and depends on no context.

| Context | Type | Aggregates | May depend on | Publishes (events) |
|---|---|---|---|---|
| `shared` | kernel | — | — | — |
| `iam` | generic | `User`, `LoginCode`, `Session` | — | — |
| `organization` | supporting | `Organization`, `Establishment`, `Member`, `Service` | `iam` | — |
| `customers` | supporting | `Customer` | — | `CustomerDeleted` |
| `booking` | core | `Appointment`, `TimeBlock` | `organization`, `customers` | `AppointmentBooked`, `AppointmentRescheduled`, `AppointmentCancelled` |
| `conversations` | core | `WhatsappLine`, `Conversation`, `Message`, `ScheduledMessage` | `organization`, `customers`, `booking` (events, [templates](decisions/implemented/2026-10-22-staff-templates-read-appointments-from-booking.md)) | `InboundMessageReceived` |
| `assistant` | core | `AssistantSettings`, `AssistantTurn` | `organization`, `customers`, `booking`, `conversations` | — |

`ArchitectureTest.ALLOWED_DEPENDENCIES` mirrors the "May depend on" column; an unmapped package fails the build. Change the table, the map and, for a new context, a decision record in the same commit.

The shared kernel holds what several contexts use: typed identifiers (`OrganizationId`, `EstablishmentId`, `MemberId`, `CustomerId`, `UserId`, `ServiceId`), `Money`, `TimeRange`, `IdempotencyKey`, `Email`, `SecretHash`, `Role`, `TenantContext` with `AccessPolicy`, the domain exceptions with `GlobalExceptionMapper`, the base persistence entity, the Resend client and the UTC `Clock`.

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
- **Ask up:** a lower context declares a `<Answer>Provider` in its `interfaces.acl` and a higher one implements it ([decision](decisions/implemented/2026-10-21-lower-contexts-ask-up-through-providers.md)).
- **Calls go down, events go up:** a context calls only contexts it may depend on. When a lower context must trigger work in a higher one, it publishes an event. There are no cycles.

| Event | Listener | Phase | What the listener does |
|---|---|---|---|
| `AppointmentBooked`, `AppointmentRescheduled`, `AppointmentCancelled` | `conversations` | in transaction | Schedules, moves or cancels confirmation and reminder rows |
| `CustomerDeleted` | `conversations` | in transaction | Marks the customer's media for the retention job |
| `InboundMessageReceived` | `assistant` | in transaction | Enqueues an assistant turn when the assistant may answer |
| `InboundMessageReceived` | `assistant` | after commit | Wakes the turn worker |

In-transaction listeners write only their own tables, so a failure rolls back everything. After-commit listeners only wake work up; a polling job recovers anything lost. `ConversationChanged` and `AssistantActivity` feed the SSE inbox stream as CDI events inside `conversations`. Event names above omit the `IntegrationEvent` suffix. The detail and templates are in [takodu-context-integration](../.agents/skills/takodu-context-integration/SKILL.md).

## Requests, tenancy and background work

A request passes JWT verification, then `organization`'s `TenantFilter` loads the caller's active membership for the `X-Organization-Id` header into `TenantContext` or answers 403. `/internal/v1` is for the Takodu team: `iam`'s `OperatorFilter` admits only platform operators ([decision](decisions/implemented/2026-10-23-takodu-team-as-platform-operators.md)). Command services check `AccessPolicy` before loading the aggregate. Webhooks and jobs have no `TenantContext`; they derive the organization from their data. The rules are in the [tenancy decision](decisions/implemented/2026-10-19-tenancy-and-fixed-roles.md).

Jobs claim rows with `SELECT … FOR UPDATE SKIP LOCKED`; outgoing messages, media and assistant turns are rows ([decision](decisions/implemented/2026-10-19-webhooks-store-first-and-polled-outbox.md)). Every external system (Meta, the LLM provider, Resend) sits behind a port in `application.internal.outboundservices`.

## Persistence

Each table belongs to one context; foreign keys across contexts are allowed, but no context reads or writes another context's table. Invariants that concurrency can break are also database constraints, and the repository adapter translates their SQL state into a domain exception ([decision](decisions/implemented/2026-10-19-database-enforces-concurrent-invariants.md)). Migrations follow the [migrations decision](decisions/implemented/2026-10-19-schema-migrations.md).

## HTTP errors

| Exception | Status |
|---|---|
| `IllegalArgumentException` (invalid value object or command) | 400 |
| `AuthenticationFailedException` (missing or wrong credentials) | 401 |
| `PlanInactiveException` | 402 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `BusinessRuleViolationException` | 422 |
| `TooManyRequestsException` | 429 |
| Anything else | 500 |

The body is `{"code": "...", "message": "..."}`. Bean Validation rejections are 400 in Quarkus's violations format. The reason is in the [errors decision](decisions/implemented/2026-10-19-errors-with-exception-mapper.md).
