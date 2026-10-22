package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/** One opening range in the establishment's local time (O8). */
public record OpeningRange(DayOfWeek day, LocalTime opensAt, LocalTime closesAt) {
    public OpeningRange {
        Objects.requireNonNull(day, "day is required");
        Objects.requireNonNull(opensAt, "opensAt is required");
        Objects.requireNonNull(closesAt, "closesAt is required");
    }
}
