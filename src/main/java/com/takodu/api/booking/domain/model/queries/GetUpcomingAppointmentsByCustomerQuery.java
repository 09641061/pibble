package com.takodu.api.booking.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;

/** The customer's confirmed appointments at the establishment that start after {@code now}, earliest first. */
public record GetUpcomingAppointmentsByCustomerQuery(OrganizationId organizationId, EstablishmentId establishmentId,
                                                     CustomerId customerId, Instant now) {
    public GetUpcomingAppointmentsByCustomerQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (now == null) throw new IllegalArgumentException("now is required");
    }
}
