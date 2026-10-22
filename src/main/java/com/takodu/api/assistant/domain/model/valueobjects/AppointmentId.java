package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.UUID;

/** @param value booking's id of one of the customer's appointments; never shown to the model */
public record AppointmentId(UUID value) {
    public AppointmentId {
        if (value == null) throw new IllegalArgumentException("AppointmentId is required");
    }
}
