package com.takodu.api.organization.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

/**
 * O8: the weekly opening hours of an establishment. A weekday without ranges is closed; several ranges in one day
 * allow split shifts.
 *
 * @param ranges sorted by weekday and opening time, never overlapping
 */
public record WeeklySchedule(List<OpeningRange> ranges) {
    public static final WeeklySchedule CLOSED = new WeeklySchedule(List.of());

    public WeeklySchedule {
        if (ranges == null) throw new IllegalArgumentException("Opening ranges are required");
        ranges = ranges.stream()
                .sorted(Comparator.comparing(OpeningRange::day).thenComparing(OpeningRange::opensAt))
                .toList();
        for (int i = 1; i < ranges.size(); i++)
            if (ranges.get(i - 1).overlaps(ranges.get(i)))
                throw new IllegalArgumentException("Opening ranges overlap on " + ranges.get(i).day());
    }

    /** @return the ranges of the weekday, earliest first; empty when closed */
    public List<OpeningRange> on(DayOfWeek day) {
        return ranges.stream().filter(range -> range.day() == day).toList();
    }
}
