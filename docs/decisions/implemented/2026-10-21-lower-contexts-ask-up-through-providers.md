# Decision: Lower contexts ask up through provider interfaces

Status: implemented

## Problem

A rule in a lower context can depend on data a higher context owns. `organization` must refuse to remove a member who still has upcoming appointments, so that no booked customer is left without a person, but appointments belong to `booking`, and `booking` depends on `organization`, not the other way round. Facade calls go down and events go up; neither lets `organization` ask `booking` a question and wait for the answer.

## Decision

The lower context declares a provider interface in its own `interfaces.acl`, named `<Answer>Provider` and using only plain types (`organization.interfaces.acl.UpcomingAppointmentsProvider`). Its command service calls the interface. The higher context implements it in `application.internal.outboundservices.acl`, the only place where a context may touch another context's `interfaces.acl`, so the dependency still points from `booking` to `organization` and `ArchitectureTest` needs no new rule. Until the higher context exists, the lower context ships a `@DefaultBean` with the answer that is true without it (`NoUpcomingAppointments`: nobody has appointments before booking exists). Tests replace it with an `@Alternative` fake.

## Alternatives considered

**Let `organization` call `booking`'s facade.** It adds the edge `organization → booking` to the context map, which closes a cycle with `booking → organization`.

**Check the rule in `booking`.** `booking` would offer "remove member" and call `organization` to remove; the removal of a member is an `organization` decision, and the endpoint would live in the wrong context.

**A projection in `organization` fed by booking events.** `organization` would listen to `AppointmentBooked` and keep counts per member, which needs the same forbidden edge and a second copy of appointment state that can drift.

## Consequences

The context map and the cycle check stay as they are. Each provider is one more interface for the higher context to implement; forgetting it is safe only while the default answer is true, so a context that starts owning the data must replace the default in the same pull request.
