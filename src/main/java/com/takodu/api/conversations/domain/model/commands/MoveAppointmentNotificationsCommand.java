package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Instant;
import java.util.Objects;

/** @param startsAt the appointment's new start */
public record MoveAppointmentNotificationsCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                                  AppointmentId appointmentId, CustomerId customerId,
                                                  ServiceId serviceId, Instant startsAt) {
    public MoveAppointmentNotificationsCommand {
        Objects.requireNonNull(organizationId, "organizationId is required");
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(appointmentId, "appointmentId is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
    }
}
