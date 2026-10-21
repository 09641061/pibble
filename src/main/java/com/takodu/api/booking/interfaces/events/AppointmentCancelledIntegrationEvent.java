package com.takodu.api.booking.interfaces.events;

import java.time.Instant;
import java.util.UUID;

/**
 * An appointment was cancelled; conversations cancels its pending reminders.
 *
 * @param startsAt when the appointment would have started
 * @param cancelledBy {@code staff} or {@code customer}
 */
public record AppointmentCancelledIntegrationEvent(UUID organizationId, UUID appointmentId, UUID establishmentId,
                                                   UUID customerId, Instant startsAt, String cancelledBy,
                                                   String reason) { }
