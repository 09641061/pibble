package com.takodu.api.shared.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * A half-open range of instants {@code [start, end)}, so back-to-back appointments do not overlap.
 *
 * @param start first instant inside the range
 * @param end first instant after the range; later than {@code start}
 */
public record TimeRange(Instant start, Instant end) {
    public TimeRange {
        if (start == null) throw new IllegalArgumentException("start is required");
        if (end == null) throw new IllegalArgumentException("end is required");
        if (!end.isAfter(start)) throw new IllegalArgumentException("end must be after start");
    }

    /**
     * @param start first instant inside the range
     * @param length how long the range lasts; positive
     * @return the range from {@code start} to {@code start + length}
     */
    public static TimeRange of(Instant start, Duration length) {
        if (start == null) throw new IllegalArgumentException("start is required");
        if (length == null) throw new IllegalArgumentException("length is required");
        return new TimeRange(start, start.plus(length));
    }

    /**
     * @param other the range to compare with
     * @return whether both ranges share at least one instant (B5, B6)
     */
    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /**
     * @return the duration between start and end
     */
    public Duration length() {
        return Duration.between(start, end);
    }
}
