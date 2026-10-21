package com.takodu.api.booking.domain.model.queries;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public record GetAppointmentByIdQuery(OrganizationId organizationId, AppointmentId appointmentId) {
    public GetAppointmentByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
    }
}
