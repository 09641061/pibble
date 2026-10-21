---
name: takodu-context-integration
description: How bounded contexts in the pibble Quarkus backend talk to each other — anti-corruption layer (ACL) facades with view records, consumer-side External<Context>Service translators, CDI integration events, and the context map that says which context may depend on which. Use whenever code in one context needs data or behavior from another, when adding a facade method or integration event, or when ArchitectureTest reports contexts_meet_only_through_acl_and_events, contexts_depend_only_along_the_context_map or no_cycles_between_contexts.
---

# Integrating pibble bounded contexts

Two mechanisms exist, and nothing else crosses a context boundary. `ArchitectureTest` rejects any other dependency between contexts. Package names below are relative to `com.takodu.api`.

| Need | Mechanism | Direction |
|---|---|---|
| Ask another context for data, or tell it to do something now and get the result | **ACL facade** call | Consumer depends on supplier |
| Let other contexts react to a fact that already happened | **Integration event** | Listener depends on publisher; publisher knows no listener |

Choose the facade when the caller needs the answer to continue. Choose an event when the caller is done and others only follow up.

## The context map

A context may depend only on the contexts in its "May depend on" column in [docs/architecture.md](../../../docs/architecture.md), mirrored by `ArchitectureTest.ALLOWED_DEPENDENCIES`. Calls go down (`assistant → booking → organization → identity`); when a lower context must trigger work in a higher one, it publishes an event that the higher one observes. `booking` never calls `conversations`: `conversations` listens to `AppointmentBookedIntegrationEvent`. Adding an edge to the map is a design change: update the table, the map and a decision record in the same commit, and never add an edge that creates a cycle.

## ACL facade (supplier side)

1. `<supplier>.interfaces.acl.<Supplier>ContextFacade`: an **interface**. Methods use primitives, `String`, `UUID`, `Instant`, `LocalDate`, `LocalTime`, `Optional`, `List`, and view records. Never return an aggregate, entity, or value object, and never return sentinel values such as `0L` or `""`: use `Optional`.
2. `<supplier>.interfaces.acl.<Thing>View`: immutable records of plain types.
3. `<supplier>.application.acl.<Supplier>ContextFacadeImpl`: `@ApplicationScoped`, implements the interface by building commands and queries and calling the supplier's own command and query services. No repository access and no business rules here.
4. Every method takes the `organizationId` it acts in, so the supplier keeps its tenant scoping.

```java
package com.takodu.api.organization.interfaces.acl;

public interface OrganizationContextFacade {
    /** @return the caller's active membership in the organization, empty when none exists */
    Optional<MembershipView> fetchActiveMembership(UUID organizationId, UUID userId);

    /** @throws EstablishmentNotFoundException when the establishment is not in the organization */
    EstablishmentCalendarView fetchCalendar(UUID organizationId, UUID establishmentId);

    Optional<ServiceView> fetchService(UUID organizationId, UUID serviceId);
}

public record MembershipView(UUID memberId, String role, Set<UUID> establishmentIds) { }
public record EstablishmentCalendarView(UUID establishmentId, String timeZone, List<OpeningRangeView> openingHours,
                                        boolean planActive) { }
public record OpeningRangeView(int isoWeekday, LocalTime opensAt, LocalTime closesAt) { }
public record ServiceView(UUID id, UUID establishmentId, String name, int durationMinutes, long priceCents,
                          String currency, boolean active, Set<UUID> performerIds) { }
```

```java
package com.takodu.api.organization.application.acl;

@ApplicationScoped
public class OrganizationContextFacadeImpl implements OrganizationContextFacade {
    private final ServiceQueryService serviceQueryService;

    // constructor injection; other query services omitted

    @Override
    public Optional<ServiceView> fetchService(UUID organizationId, UUID serviceId) {
        return serviceQueryService.handle(new GetServiceByIdQuery(new OrganizationId(organizationId), new ServiceId(serviceId)))
                .map(s -> new ServiceView(s.getId().value(), s.getEstablishmentId().value(), s.getName(),
                        (int) s.getDuration().toMinutes(), s.getPrice().cents(), s.getPrice().currency(),
                        s.isActive(), s.getPerformerIds().stream().map(MemberId::value).collect(toSet())));
    }
}
```

## Consumer side

`<consumer>.application.internal.outboundservices.acl.External<Supplier>Service` is the **only** class in the consumer that may import the supplier's facade. It translates views into the consumer's own value objects, so a supplier change touches one class.

```java
package com.takodu.api.booking.application.internal.outboundservices.acl;

@ApplicationScoped
public class ExternalOrganizationService {
    private final OrganizationContextFacade organizationContextFacade;

    public ExternalOrganizationService(OrganizationContextFacade organizationContextFacade) {
        this.organizationContextFacade = organizationContextFacade;
    }

    /** @throws ServiceNotBookableException when the service does not exist in the organization */
    public ServiceOffer fetchServiceOffer(OrganizationId organizationId, ServiceId serviceId) {
        var view = organizationContextFacade.fetchService(organizationId.value(), serviceId.value())
                .orElseThrow(() -> new ServiceNotBookableException(serviceId));
        return new ServiceOffer(serviceId, new EstablishmentId(view.establishmentId()),
                Duration.ofMinutes(view.durationMinutes()), new Money(view.priceCents(), view.currency()),
                view.active(), view.performerIds().stream().map(MemberId::new).collect(toSet()));
    }
}
```

`ServiceOffer` is a value object in `booking.domain.model.valueobjects`, and `ServiceNotBookableException` is a `booking` exception: the consumer's domain never sees the supplier's types. Command services and assistant tools call `ExternalOrganizationService`, never the facade.

## Integration events

1. The publisher defines `<publisher>.interfaces.events.<Fact>IntegrationEvent`: a record of plain types named for a past fact (`AppointmentBookedIntegrationEvent`, `InboundMessageReceivedIntegrationEvent`) that carries the `organizationId`, because listeners have no `TenantContext`.
2. The publisher's command service injects `Event<<Fact>IntegrationEvent>` and calls `fire(...)` after `save` returns, inside the same transaction.
3. Each listener is `<listener>.application.internal.eventhandlers.<Fact>EventHandler`, `@ApplicationScoped`, with one `void on(@Observes <Fact>IntegrationEvent event)` method that builds a command of its own context and calls its own command service.

```java
package com.takodu.api.conversations.application.internal.eventhandlers;

@ApplicationScoped
public class AppointmentBookedEventHandler {
    private final ScheduledMessageCommandService scheduledMessageCommandService;

    public AppointmentBookedEventHandler(ScheduledMessageCommandService scheduledMessageCommandService) {
        this.scheduledMessageCommandService = scheduledMessageCommandService;
    }

    /** W11, W12: schedules the confirmation and reminder rows. Idempotent: one row per appointment and kind. */
    void on(@Observes AppointmentBookedIntegrationEvent event) {
        scheduledMessageCommandService.handle(new ScheduleAppointmentNotificationsCommand(
                new OrganizationId(event.organizationId()), event.appointmentId(), event.startsAt(), event.source()));
    }
}
```

Timing rules:

- `@Observes` runs synchronously in the publisher's transaction: if the handler throws, the whole operation rolls back. Use it to insert work rows (scheduled messages, assistant turns) that must exist together with the fact, keep handlers idempotent, and write only the listener's own tables.
- `@Observes(during = TransactionPhase.AFTER_SUCCESS)` runs after commit and cannot roll the publisher back. Use it only to wake a job; the job's polling recovers a lost wake-up.
- Never call Meta, the LLM or Resend from a handler; insert a row and let a job do it.
- Never put business logic in the event record or fire an event from a controller, repository adapter, or aggregate.

## Checklist

- [ ] The dependency is allowed by the context map, and no cycle appears.
- [ ] The facade is an interface in `interfaces.acl`, implemented in `application.acl`, and every method takes the `organizationId`.
- [ ] Facade methods return views or plain types, with `Optional` for "may not exist".
- [ ] Only `External<Supplier>Service` imports the facade; only `*EventHandler` classes import another context's events.
- [ ] Event names are past-tense facts, end with `IntegrationEvent` and carry the `organizationId`.
- [ ] `ArchitectureTest` passes.
