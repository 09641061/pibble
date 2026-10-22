package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param source who cancels: staff in the app, or the assistant for its customer, which B14 limits */
public record CancelAppointmentCommand(OrganizationId organizationId, AppointmentId appointmentId, CancelReason reason,
                                       CancelledBy cancelledBy, BookingSource source, IdempotencyKey idempotencyKey) {
    public CancelAppointmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        if (reason == null) throw new IllegalArgumentException("A cancel reason is required");
        if (cancelledBy == null) throw new IllegalArgumentException("cancelledBy is required");
        if (source == null) throw new IllegalArgumentException("source is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
