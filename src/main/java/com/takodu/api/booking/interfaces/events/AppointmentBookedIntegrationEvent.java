package com.takodu.api.booking.interfaces.events;

import java.time.Instant;
import java.util.UUID;

/**
 * An appointment was booked; conversations schedules its confirmation and reminders from it.
 *
 * @param source {@code staff} or {@code assistant}
 */
public record AppointmentBookedIntegrationEvent(UUID organizationId, UUID appointmentId, UUID establishmentId,
                                                UUID customerId, UUID staffMemberId, UUID serviceId, Instant startsAt,
                                                Instant endsAt, String source) { }
