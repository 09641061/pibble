package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Instant;
import java.util.Objects;

/** @param bookedByStaff false when the assistant booked it, whose reply in the chat is the confirmation (W12) */
public record ScheduleAppointmentNotificationsCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                                      AppointmentId appointmentId, CustomerId customerId,
                                                      ServiceId serviceId, Instant startsAt, boolean bookedByStaff) {
    public ScheduleAppointmentNotificationsCommand {
        Objects.requireNonNull(organizationId, "organizationId is required");
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(appointmentId, "appointmentId is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
    }
}
