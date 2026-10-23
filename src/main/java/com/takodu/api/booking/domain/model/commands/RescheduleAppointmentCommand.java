package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;

/** @param staffMemberId the new member; null keeps the current one */
public record RescheduleAppointmentCommand(OrganizationId organizationId, AppointmentId appointmentId,
                                           MemberId staffMemberId, Instant start, BookingSource source,
                                           IdempotencyKey idempotencyKey) {
    public RescheduleAppointmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (appointmentId == null) throw new IllegalArgumentException("appointmentId is required");
        if (start == null) throw new IllegalArgumentException("start is required");
        if (source == null) throw new IllegalArgumentException("source is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
