package com.takodu.api.assistant.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Instant;
import java.util.Objects;

/**
 * One of the customer's upcoming appointments at the establishment.
 *
 * @param mayChange whether the customer may still reschedule or cancel it through the assistant (B14)
 */
public record CustomerAppointment(AppointmentId id, ServiceId serviceId, MemberId staffMemberId, Instant start,
                                  boolean mayChange) {
    public CustomerAppointment {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        Objects.requireNonNull(staffMemberId, "staffMemberId is required");
        Objects.requireNonNull(start, "start is required");
    }
}
