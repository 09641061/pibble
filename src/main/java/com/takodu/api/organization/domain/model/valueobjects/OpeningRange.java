package com.takodu.api.organization.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * O8: one range of a weekday in which the establishment accepts appointments, in its local time.
 *
 * @param opensAt whole minutes
 * @param closesAt after {@code opensAt} on the same day; a range never crosses midnight, so 23:59 is the latest
 */
public record OpeningRange(DayOfWeek day, LocalTime opensAt, LocalTime closesAt) {
    public OpeningRange {
        if (day == null) throw new IllegalArgumentException("Weekday is required");
        if (opensAt == null || closesAt == null) throw new IllegalArgumentException("Opening and closing times are required");
        if (opensAt.getSecond() != 0 || opensAt.getNano() != 0 || closesAt.getSecond() != 0 || closesAt.getNano() != 0)
            throw new IllegalArgumentException("Opening hours use whole minutes");
        if (!closesAt.isAfter(opensAt))
            throw new IllegalArgumentException("A range must close after it opens, on the same day; close at 23:59 at the latest");
    }

    public boolean overlaps(OpeningRange other) {
        return day == other.day && opensAt.isBefore(other.closesAt) && other.opensAt.isBefore(closesAt);
    }
}
