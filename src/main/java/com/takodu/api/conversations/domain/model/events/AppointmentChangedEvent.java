package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Booking committed an appointment change, forwarded so open calendars reload without polling. Not an integration
 * event: conversations builds it from booking's.
 */
public record AppointmentChangedEvent(OrganizationId organizationId, EstablishmentId establishmentId,
                                      AppointmentId appointmentId, Change change) implements InboxEvent {
    public enum Change { BOOKED, RESCHEDULED, CANCELLED }
}
