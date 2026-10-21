package com.takodu.api.booking.domain.services;

import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.Slot;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** B16: the start times, every 15 minutes, at which a service can be booked on one local date. Pure. */
public final class SlotFinder {
    static final Duration STEP = Duration.ofMinutes(15);

    private final AvailabilityPolicy availabilityPolicy;

    public SlotFinder(AvailabilityPolicy availabilityPolicy) {
        this.availabilityPolicy = availabilityPolicy;
    }

    /**
     * Slots start inside an opening window, at its opening time plus a multiple of 15 minutes, and not before
     * {@link BookingContext#now()}; staff record a walk-in by booking it directly.
     *
     * @param context loaded for the whole local date
     * @param customer whose appointments must not overlap; null when nobody is booking yet
     * @param member the only member to consider; null for every performer
     */
    public List<Slot> find(BookingContext context, LocalDate date, CustomerId customer, MemberId member,
                           BookingSource source) {
        var calendar = context.calendar();
        var duration = context.service().duration();
        var slots = new ArrayList<Slot>();
        for (var window : calendar.windowsOn(date)) {
            var opensAt = date.atTime(window.opensAt()).atZone(calendar.zone()).toInstant();
            var closesAt = date.atTime(window.closesAt()).atZone(calendar.zone()).toInstant();
            for (var start = opensAt; !start.plus(duration).isAfter(closesAt); start = start.plus(STEP)) {
                if (start.isBefore(context.now())) continue;
                var range = TimeRange.of(start, duration);
                var free = availabilityPolicy.freeMembers(context, customer, range, source).stream()
                        .filter(candidate -> member == null || candidate.equals(member))
                        .toList();
                if (!free.isEmpty()) slots.add(new Slot(range, free));
            }
        }
        return slots;
    }
}
