package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.UUID;

/** @param value booking's id of the appointment the notification is about */
public record AppointmentId(UUID value) {
    public AppointmentId {
        if (value == null) throw new IllegalArgumentException("AppointmentId is required");
    }
}
