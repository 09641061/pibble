package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public record CancelAppointmentCommand(OrganizationId organizationId, AppointmentId appointmentId, CancelReason reason,
                                       CancelledBy cancelledBy, IdempotencyKey idempotencyKey) {
    public CancelAppointmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        if (reason == null) throw new IllegalArgumentException("A cancel reason is required");
        if (cancelledBy == null) throw new IllegalArgumentException("cancelledBy is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
