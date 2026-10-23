package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Whole days, both included, read in each establishment's time zone (O7), so "October" means October in Lima.
 *
 * @param from the first day
 * @param to the last day
 */
public record UsagePeriod(LocalDate from, LocalDate to) {
    public static final int MAX_DAYS = 366;

    public UsagePeriod {
        if (from == null || to == null) throw new IllegalArgumentException("from and to are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to must not be before from");
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS)
            throw new IllegalArgumentException("A usage period spans at most " + MAX_DAYS + " days");
    }

    /** @return the first instant of {@code from} in the zone */
    public Instant start(ZoneId zone) {
        return from.atStartOfDay(zone).toInstant();
    }

    /** @return the first instant after {@code to} in the zone */
    public Instant end(ZoneId zone) {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** @return a range that holds the period in every time zone, to find which establishments had turns */
    public Instant earliestStart() {
        return start(ZoneId.of("Etc/GMT-14"));
    }

    /** @return the end of the period in the westernmost time zone */
    public Instant latestEnd() {
        return end(ZoneId.of("Etc/GMT+12"));
    }
}
