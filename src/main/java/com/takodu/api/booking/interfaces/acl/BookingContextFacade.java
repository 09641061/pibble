package com.takodu.api.booking.interfaces.acl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the assistant may do in booking, for the customer it is talking to. Every method acts as the assistant: the
 * caller binds it to its establishment with {@code TenantContext.bindAssistant} first, bookings carry
 * {@code source=assistant} (B9) and changes stop 2 hours before the start (B14). Expected failures are the shared
 * kernel's domain exceptions, with booking's codes such as {@code SLOT_UNAVAILABLE} or
 * {@code OUTSIDE_BOOKING_WINDOW}.
 */
public interface BookingContextFacade {
    /**
     * B16 with B9: the times the assistant may book on a local date, earliest first.
     *
     * @param staffMemberId only this member's slots; null for every performer
     */
    List<SlotView> fetchSlots(UUID organizationId, UUID serviceId, UUID customerId, LocalDate date,
                              UUID staffMemberId);

    /**
     * B1-B9, B17: books for the customer; a repeated key returns the first appointment.
     *
     * @param staffMemberId the member the customer asked for; null lets booking choose (B17)
     */
    AppointmentView book(UUID organizationId, UUID customerId, UUID serviceId, UUID staffMemberId, Instant start,
                         UUID idempotencyKey);

    /** @return the customer's confirmed appointments at the establishment that have not started, earliest first */
    List<AppointmentView> fetchUpcomingAppointments(UUID organizationId, UUID establishmentId, UUID customerId);

    /**
     * B13, B14: moves one of the customer's appointments, keeping its member; a repeated key changes nothing.
     *
     * @throws com.takodu.api.shared.domain.exceptions.ResourceNotFoundException when it is not this customer's
     */
    AppointmentView reschedule(UUID organizationId, UUID customerId, UUID appointmentId, Instant start,
                               UUID idempotencyKey);

    /**
     * B12, B14, decision 63: the customer cancels one of their appointments; a repeated key changes nothing.
     *
     * @throws com.takodu.api.shared.domain.exceptions.ResourceNotFoundException when it is not this customer's
     */
    AppointmentView cancel(UUID organizationId, UUID customerId, UUID appointmentId, String reason,
                           UUID idempotencyKey);
}
