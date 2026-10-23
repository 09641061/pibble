package com.takodu.api.booking.domain.model.valueobjects;

/** B12: why an appointment was cancelled, at most 500 characters. */
public record CancelReason(String value) {
    public static final int MAX_LENGTH = 500;

    public CancelReason {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A cancel reason is required");
        value = value.strip();
        if (value.length() > MAX_LENGTH) throw new IllegalArgumentException("Cancel reason must have at most 500 characters");
    }
}
