package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.time.Instant;

/**
 * The establishment is the service's (B1).
 *
 * @param staffMemberId the chosen member; null lets booking choose one (B17)
 * @param createdBy the signed-in user for staff bookings, from {@code TenantContext}; null for the assistant
 */
public record BookAppointmentCommand(OrganizationId organizationId, CustomerId customerId, ServiceId serviceId,
                                     MemberId staffMemberId, Instant start, BookingSource source, UserId createdBy,
                                     IdempotencyKey idempotencyKey) {
    public BookAppointmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (serviceId == null) throw new IllegalArgumentException("serviceId is required");
        if (start == null) throw new IllegalArgumentException("start is required");
        if (source == null) throw new IllegalArgumentException("source is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
