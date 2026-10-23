package com.takodu.api.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A9: the turns an establishment's assistant finished on one day.
 *
 * @param date the day in the establishment's time zone (O7)
 */
public record DailyUsage(LocalDate date, UsageTotals totals) {
    public DailyUsage {
        Objects.requireNonNull(date, "date is required");
        Objects.requireNonNull(totals, "totals are required");
    }
}
