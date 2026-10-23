# Decision: Staff templates read the appointment from booking

Status: implemented

## Problem

Staff must reach a customer whose 24-hour window has closed (W6). WhatsApp then accepts only an approved template, and Takodu's templates are about one appointment: a reminder or a confirmation. The message must carry the appointment's current service and time and must refuse an appointment that was cancelled, moved to another customer's establishment or already started. Until now conversations learned about appointments only from booking's events.

## Decision

`POST /api/v1/conversations/{id}/templates` takes the template and an appointment id. Conversations asks booking through `ExternalBookingService` → `BookingContextFacade.fetchUpcomingAppointments` for the customer's confirmed, not started appointments at the conversation's establishment, and accepts the id only if it is among them. Notifications keep working from events and `scheduled_messages` rows; the facade call is used only for this staff action. The context map already allowed conversations to depend on booking; the table in [architecture](../../architecture.md) now names this use.

## Alternatives considered

**Read `scheduled_messages`.** No new dependency, but the rows do not describe every appointment: none exists for one booked less than 3 hours ahead, a reminder cancelled because the appointment moved too close keeps the old time, and a cancelled row does not say whether the appointment itself was cancelled. Staff would see templates refused or sent with a wrong time.

**Store a copy of each appointment in conversations from the events.** Accurate, but a second table of appointments to keep in step for one button in the inbox.

**Let staff write the template's text freely.** WhatsApp does not allow free text outside the window; only reviewed templates go out.

## Consequences

Conversations calls booking in one request path, inside the caller's tenant: booking's own establishment check applies to the staff member. Booking stays free of conversations, so there is no cycle. If booking's notion of "upcoming" changes, the template endpoint follows it.
