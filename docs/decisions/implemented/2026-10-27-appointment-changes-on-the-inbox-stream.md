# Decision: Appointment changes on the inbox stream

Status: implemented

## Problem

The calendar should show an appointment the assistant just booked, and mark it as new, without the member reloading. Booking had no way to tell an open calendar that something changed, and the only server push in pibble is `GET /api/v1/inbox/stream`, which `conversations` owns.

## Decision

`conversations` observes booking's `AppointmentBooked`, `AppointmentRescheduled` and `AppointmentCancelled` integration events after commit and forwards them on the inbox stream as `appointment_booked`, `appointment_rescheduled` and `appointment_cancelled`, with `appointmentId` and `establishmentId`, filtered by the establishments the member sees like every other event. The event carries no appointment data: the calendar reloads the day and uses `Appointment.createdAt` and `source` to decide what to mark as new.

## Alternatives considered

**A calendar stream in `booking`** (`GET /api/v1/calendar/stream`). Cleaner ownership, but it duplicates the SSE plumbing (tenant filter on a blocking stream, pings, broadcast per instance) and the app would hold two long connections per tab for one screen pair.

**Polling the calendar** every 30 seconds while it is open. Nothing to build in pibble, but most polls return nothing, and a booking still shows up to half a minute late.

**Only `createdAt`, no event.** The calendar would mark appointments as new when it happens to reload, which is rarely right after the assistant books.

## Consequences

The inbox stream is the app's single live channel; its name stays for compatibility. Every member who follows it receives appointment events for their establishments even when no calendar is open, a few bytes each. Like the rest of the stream, events reach only clients connected to the instance that committed the change; a reconnect reloads.
