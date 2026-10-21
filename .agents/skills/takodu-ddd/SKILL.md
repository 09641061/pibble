---
name: takodu-ddd
description: The DDD and layering standard for the pibble Quarkus backend. Use before creating, moving, renaming, or reviewing any Java class under src/main/java, including every feature, fix, refactor, and code review, to decide which bounded context, layer, package, and name a class gets and which other classes it may depend on.
---

# Pibble DDD standard

Pibble is a modular monolith. Each bounded context (`identity`, `organization`, `customers`, `booking`, `conversations`, `assistant`) is a direct subpackage of `com.takodu.api` with the same four layers; [docs/architecture.md](../../../docs/architecture.md) lists what each owns and which contexts it may depend on. `ArchitectureTest` enforces the dependency and naming rules marked **[enforced]**; review enforces the rest. When this skill and the code disagree, the code is wrong unless a decision record in `docs/decisions/implemented/` says otherwise. Business rules come from the product docs (`../docs/03-business-domain.md`); cite their id (`B3`) in the Javadoc of the method that enforces them.

Use [takodu-feature](../takodu-feature/SKILL.md) for the step order of a new capability, [takodu-fix](../takodu-fix/SKILL.md) for defects, [takodu-context-integration](../takodu-context-integration/SKILL.md) whenever code in one context needs another context, and [takodu-tenancy](../takodu-tenancy/SKILL.md) for anything that reads the caller's organization. Copy class shapes from [references/templates.md](references/templates.md).

## Context skeleton

```text
com.takodu.api.<context>
├── domain
│   ├── model
│   │   ├── aggregates        <Aggregate>
│   │   ├── entities          <Entity> (lives inside one aggregate)
│   │   ├── valueobjects      <ValueObject>, enums
│   │   ├── commands          <Verb><Aggregate>Command
│   │   └── queries           Get<Aggregate>By<Field>Query, Search<Aggregates>Query
│   ├── exceptions            <Aggregate>NotFoundException, …
│   ├── services              domain services (pure, no I/O), e.g. AvailabilityPolicy
│   └── repositories          <Aggregate>Repository (port)
├── application
│   ├── commandservices       <Aggregate>CommandService
│   ├── queryservices         <Aggregate>QueryService
│   ├── acl                   <Context>ContextFacadeImpl
│   └── internal
│       ├── commandservices   <Aggregate>CommandServiceImpl
│       ├── queryservices     <Aggregate>QueryServiceImpl
│       ├── eventhandlers     <Fact>EventHandler
│       ├── jobs              @Scheduled workers, e.g. ScheduledMessageDispatcher, TurnWorker
│       ├── tools             assistant only: LangChain4j @Tool classes
│       └── outboundservices
│           ├── acl           External<OtherContext>Service
│           └── <capability>  port to an external system, e.g. whatsapp/WhatsappGateway
├── infrastructure
│   ├── persistence/jpa
│   │   ├── entities          <Aggregate>PersistenceEntity
│   │   ├── repositories      <Aggregate>PersistenceRepository (Panache)
│   │   ├── assemblers        <Aggregate>PersistenceAssembler
│   │   ├── adapters          <Aggregate>RepositoryImpl
│   │   └── converters        <ValueObject>PersistenceConverter
│   └── <technology>          adapters for application ports, e.g. meta, llm, resend
└── interfaces
    ├── rest                  <Aggregates>Controller, <Parent><Children>Controller
    │   ├── resources         <Aggregate>Resource, Register<Aggregate>Resource, …
    │   ├── transform         <X>CommandFromResourceAssembler, <Aggregate>ResourceFromEntityAssembler
    │   └── filters           request filters, e.g. organization's TenantFilter
    ├── acl                   <Context>ContextFacade, <Thing>View
    └── events                <Fact>IntegrationEvent
```

`com.takodu.api.shared` is the shared kernel. It uses the same layers, holds only what several contexts need (domain exceptions, the error mapper, the base persistence entity, typed identifiers such as `OrganizationId`, `Money`, `TimeRange`, and `application.tenancy.TenantContext` with `AccessPolicy`), and depends on no context **[enforced]**. Create a subpackage only when it gets its first class. A package under `com.takodu.api` that is not on the context map fails the build **[enforced]**.

## Dependency rules

The arrows point inward: `interfaces → application → domain ← infrastructure`.

1. **Domain is plain Java [enforced].** No `jakarta.*`, `io.quarkus.*`, `org.hibernate.*`, Jackson, MicroProfile, JBoss, or `dev.langchain4j.*` imports. Lombok `@Getter` is allowed. A domain class must be unit-testable with `new`.
2. **Domain depends on no outer layer [enforced].**
3. **Application never touches adapters or HTTP [enforced].** It depends on domain types, repository ports, and outbound-service ports. CDI, `@Transactional`, and `Event<T>` are allowed here.
4. **Infrastructure never touches `interfaces` [enforced].** It implements domain repository ports and application outbound ports.
5. **Interfaces translate, nothing else.** Controllers build commands and queries through assemblers, call one service, and assemble the response. No repository access, no business decisions, no `@Transactional`.
6. **Contexts meet only at `interfaces.acl` and `interfaces.events` [enforced]**, and only along the context map **[enforced]**. See [takodu-context-integration](../takodu-context-integration/SKILL.md). No cycles between contexts **[enforced]**.

## Building blocks

- **Value objects** are records that validate and normalize in the compact constructor and throw `IllegalArgumentException` with a client-readable message (the mapper turns it into 400). Wrap every field that has a rule (`PhoneNumber`, `Email`, `CancelReason`) and every identifier (`CustomerId(UUID value)`). Money is `Money(long cents, String currency)`; instants are UTC `Instant`, converted to the establishment's `ZoneId` only for local-time rules (B3). The domain receives the current instant as a parameter; services read it from an injected `java.time.Clock`.
- **Aggregates** are classes with `@Getter` and no `@Setter` or public non-final field **[enforced]**, and carry the `OrganizationId` they belong to. State changes only through intention-revealing methods (`cancel(reason, cancelledBy)`, `pause(until)`) that enforce invariants and throw a domain exception on violation. Two constructors: a creation constructor (or static factory) that sets initial state, and a full rehydration constructor used only by the persistence assembler.
- **Entities** live inside one aggregate and are changed only through the root.
- **Commands and queries** are records in `domain.model`. Commands reject missing or out-of-range arguments in the compact constructor; formats belong to the value objects they carry.
- **Domain services** hold a rule that spans several aggregates. They are pure: the service loads data and passes it in.
- **Repository ports** live in `domain.repositories`, speak in aggregates and value objects, and return `Optional` for single lookups. One repository per aggregate root. **Every method takes the `OrganizationId`**, an `EstablishmentId` the caller checked, or the aggregate; there is no unscoped `findById`. Job claims and webhook lookups are `@CrossTenant("<caller>")` instead **[enforced]**.
- **Domain exceptions** live in `<context>.domain.exceptions` and extend one of the shared ones, which fixes the HTTP status:

| Shared base | Status | Use |
|---|---|---|
| `ResourceNotFoundException` | 404 | aggregate missing in the caller's organization |
| `ConflictException` | 409 | duplicate unique value, concurrent change |
| `BusinessRuleViolationException` | 422 | invariant or state transition refused |
| `AccessDeniedException` | 403 | role or establishment does not allow the action |
| `PlanInactiveException` | 402 | organization plan inactive (O6) |
| `IllegalArgumentException` | 400 | value object or command argument invalid |

Never throw `jakarta.ws.rs.*Exception`, return `null` for a business failure, or rethrow a vaguer exception. A database constraint that guards an invariant (exclusion, partial unique index) is translated by the repository adapter into the context's exception, by SQL state.

## Application services (CQRS)

- Writes go through `<Aggregate>CommandService`, reads through `<Aggregate>QueryService`. Each public method is `handle(<X>Command)` or `handle(<X>Query)`; overloads by parameter type, never `register(...)` or `searchCustomers(...)`.
- A command `handle` is `@Transactional`, checks `AccessPolicy` (and the plan when O6 applies), loads the aggregate or checks uniqueness, calls one aggregate method or factory, saves, fires the integration event if the change is a published fact, and returns the saved aggregate.
- A query `handle` returns `Optional<Aggregate>` or `List<Aggregate>` and never changes state.
- Interfaces live in `application.commandservices` / `application.queryservices`; implementations are `@ApplicationScoped` classes in `application.internal.*` with constructor injection.
- Jobs and webhooks have no `TenantContext`: they derive the `OrganizationId` from the row they process (the WhatsApp line, the appointment) and claim work with `SELECT … FOR UPDATE SKIP LOCKED`.

## Persistence

- One `<Aggregate>PersistenceEntity` per aggregate, extending `AuditableAbstractPersistenceEntity` (UUID `id`, `created_at`, `updated_at`), with explicit `@Table(name = "<plural_snake_case>")` and `@Column(name = "<snake_case>")` matching the product schema (`../docs/schema.sql`). Lombok `@Getter @Setter` is fine here.
- Value-object columns use an explicit `@Convert(converter = <ValueObject>PersistenceConverter.class)`; never `autoApply`. Enums are stored as lower-case `text` (`in_progress`) through a converter.
- `<Aggregate>PersistenceRepository implements PanacheRepositoryBase<…, UUID>` holds queries. Use `left join fetch` for collections that the assembler reads.
- `<Aggregate>RepositoryImpl` implements the domain port and maps every result through the static `<Aggregate>PersistenceAssembler`. `save` calls `persist` when the aggregate has no id and `getEntityManager().merge` otherwise, flushes when a constraint must surface, then returns the mapped entity.
- Never return a persistence entity outside `infrastructure` or put JPA annotations on a domain class.

## REST

- Controllers use plural nouns: `CustomersController` at `@Path("/api/v1/customers")`. A child collection gets its own controller: `EstablishmentOpeningHoursController` at `/api/v1/establishments/{establishmentId}/opening-hours`.
- A state transition is a POST that creates a noun: `POST /appointments/{id}/cancellations`, never `PUT /appointments/{id}?status=cancelled`.
- Request resources carry Bean Validation annotations and the controller parameter is `@Valid`. Response resources are flat records of primitives and strings.
- Every endpoint has `@Operation`, one `@APIResponse` per status it can return, and `@Tag` on the class.
- Single lookups end with `.orElseThrow(() -> new <Aggregate>NotFoundException(id))`.
- Every POST takes `@HeaderParam("Idempotency-Key") UUID` into the command's `IdempotencyKey`, or states why a repeat is harmless in `@WithoutIdempotencyKey("…")` **[enforced]**. Creations store the key and return the existing aggregate on a repeat; transitions keep `lastIdempotencyKey` and return the current state ([decision](../../../docs/decisions/proposed/2026-10-20-idempotency-keys-on-retried-requests.md)).
- Fields that grant authority (organization, role, owner, `createdBy`) never come from a request body; the controller reads them from `TenantContext` and puts them in the command.

## Review checklist

- [ ] Each new class sits in the package the skeleton prescribes and carries the prescribed name suffix.
- [ ] No setter or public field on an aggregate; every state change is a named domain method.
- [ ] Every business failure is a context exception extending the right shared base.
- [ ] Controllers contain no `if` about business state.
- [ ] Every repository port method is scoped by `OrganizationId`; no authority field comes from the request body.
- [ ] Cross-context access goes through a facade and an `External<Context>Service`, or through an integration event, along the context map.
- [ ] `./mvnw test -Dtest=ArchitectureTest` passes.
