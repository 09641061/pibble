package com.takodu.api.booking.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * O8: one range of a weekday in which an establishment accepts appointments, in its local time. It never crosses
 * midnight.
 */
public record OpeningWindow(DayOfWeek day, LocalTime opensAt, LocalTime closesAt) {
    public OpeningWindow {
        if (day == null || opensAt == null || closesAt == null)
            throw new IllegalArgumentException("Weekday, opening and closing times are required");
        if (!closesAt.isAfter(opensAt)) throw new IllegalArgumentException("A window must close after it opens");
    }

    /** @return whether {@code [from, to]} lies inside the window */
    public boolean contains(LocalTime from, LocalTime to) {
        return !from.isBefore(opensAt) && !to.isAfter(closesAt);
    }
}
