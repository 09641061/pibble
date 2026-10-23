package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Instant;
import java.util.Objects;

/**
 * @param startsAt when the appointment would have started
 * @param cancelledByStaff false when staff recorded that the customer cancelled, who needs no message (decision 63)
 * @param reason what staff wrote
 */
public record CancelAppointmentNotificationsCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                                    AppointmentId appointmentId, CustomerId customerId,
                                                    ServiceId serviceId, Instant startsAt, boolean cancelledByStaff,
                                                    String reason) {
    public CancelAppointmentNotificationsCommand {
        Objects.requireNonNull(organizationId, "organizationId is required");
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(appointmentId, "appointmentId is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
    }
}
