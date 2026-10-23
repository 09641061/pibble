package com.takodu.api.conversations.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Instant;
import java.util.Objects;

/** A customer's confirmed appointment that has not started, as booking reports it when staff send a template. */
public record UpcomingAppointment(AppointmentId appointmentId, ServiceId serviceId, Instant startsAt) {
    public UpcomingAppointment {
        Objects.requireNonNull(appointmentId, "appointmentId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
    }
}
