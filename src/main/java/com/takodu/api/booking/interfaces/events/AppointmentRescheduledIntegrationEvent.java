package com.takodu.api.booking.interfaces.events;

import java.time.Instant;
import java.util.UUID;

/** A confirmed appointment moved; conversations moves its reminders. */
public record AppointmentRescheduledIntegrationEvent(UUID organizationId, UUID appointmentId, UUID establishmentId,
                                                     UUID customerId, UUID staffMemberId, UUID serviceId,
                                                     Instant startsAt, Instant endsAt) { }
