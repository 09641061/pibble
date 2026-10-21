package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param status {@code IN_PROGRESS}, {@code COMPLETED} or {@code NO_SHOW} */
public record ChangeAppointmentStatusCommand(OrganizationId organizationId, AppointmentId appointmentId,
                                             AppointmentStatus status, IdempotencyKey idempotencyKey) {
    public ChangeAppointmentStatusCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
