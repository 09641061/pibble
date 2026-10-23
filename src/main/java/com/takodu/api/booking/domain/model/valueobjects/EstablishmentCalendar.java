package com.takodu.api.booking.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * Where and when an establishment accepts appointments, as booking sees it (B3, B7). Built by
 * {@code ExternalOrganizationService} from organization's facade.
 *
 * @param zone the establishment's time zone; opening hours are local to it (O7)
 * @param openingHours every window of the week
 * @param archived whether the establishment takes no new appointments (O4)
 * @param planActive whether the organization's plan is active (O6)
 */
public record EstablishmentCalendar(EstablishmentId establishmentId, ZoneId zone, List<OpeningWindow> openingHours,
                                    boolean archived, boolean planActive) {
    public EstablishmentCalendar {
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (zone == null) throw new IllegalArgumentException("zone is required");
        openingHours = openingHours.stream()
                .sorted(Comparator.comparing(OpeningWindow::day).thenComparing(OpeningWindow::opensAt))
                .toList();
    }

    /** B3: the range lies entirely inside one opening window of its local day. */
    public boolean isOpenDuring(TimeRange range) {
        var start = range.start().atZone(zone).toLocalDateTime();
        var end = range.end().atZone(zone).toLocalDateTime();
        if (!start.toLocalDate().equals(end.toLocalDate())) return false;
        return windowsOn(start.toLocalDate()).stream()
                .anyMatch(window -> window.contains(start.toLocalTime(), end.toLocalTime()));
    }

    /** @return the windows of the date's weekday, earliest first; empty when closed */
    public List<OpeningWindow> windowsOn(LocalDate date) {
        return openingHours.stream().filter(window -> window.day() == date.getDayOfWeek()).toList();
    }

    /** @return the instants from the date's local midnight to the next one */
    public TimeRange dayOf(LocalDate date) {
        return new TimeRange(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
