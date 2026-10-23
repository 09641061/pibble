package com.takodu.api.assistant.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * A9: an establishment's assistant usage over a period, day by day in its time zone.
 *
 * @param days only the days with answered turns, oldest first
 */
public record EstablishmentUsage(EstablishmentId establishmentId, String establishmentName, ZoneId zone,
                                 UsagePeriod period, List<DailyUsage> days) {
    public EstablishmentUsage {
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(zone, "zone is required");
        Objects.requireNonNull(period, "period is required");
        days = List.copyOf(days);
    }

    public UsageTotals totals() {
        return days.stream().map(DailyUsage::totals).reduce(UsageTotals.ZERO, UsageTotals::plus);
    }
}
