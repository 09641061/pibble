package com.takodu.api.booking.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record AppointmentId(UUID value) {
    public AppointmentId {
        if (value == null) throw new IllegalArgumentException("AppointmentId is required");
    }
}
