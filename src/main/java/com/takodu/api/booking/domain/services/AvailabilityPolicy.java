package com.takodu.api.booking.domain.services;

import com.takodu.api.booking.domain.exceptions.MemberCannotPerformServiceException;
import com.takodu.api.booking.domain.exceptions.OutsideBookingWindowException;
import com.takodu.api.booking.domain.exceptions.OutsideOpeningHoursException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.exceptions.TimeBlockedException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.shared.domain.exceptions.DomainException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * B2-B6 and B9: whether a member can take a customer's appointment in a range. Pure: the caller loads the
 * {@link BookingContext}. The plan (B7) and the service itself (B1) are checked by the caller, because they do not
 * depend on the member or the time.
 */
public final class AvailabilityPolicy {
    static final Duration ASSISTANT_MIN_LEAD = Duration.ofMinutes(30);
    static final Duration ASSISTANT_MAX_AHEAD = Duration.ofDays(60);
    static final Duration STAFF_MAX_PAST = Duration.ofHours(12);

    /**
     * @param ignoring the appointment being rescheduled, which does not collide with itself; null when booking
     * @throws MemberCannotPerformServiceException B2
     * @throws OutsideBookingWindowException B9
     * @throws OutsideOpeningHoursException B3
     * @throws TimeBlockedException B4
     * @throws SlotUnavailableException B5, B6
     */
    public void ensureBookable(BookingContext context, CustomerId customer, MemberId member, TimeRange range,
                               BookingSource source, AppointmentId ignoring) {
        var violation = firstViolation(context, customer, member, range, source, ignoring);
        if (violation != null) throw violation;
    }

    /** @return whether {@link #ensureBookable} would accept the member */
    public boolean isFree(BookingContext context, CustomerId customer, MemberId member, TimeRange range,
                          BookingSource source) {
        return firstViolation(context, customer, member, range, source, null) == null;
    }

    /**
     * B16, B17: the performers free for the range, least busy that local day first; ties keep the team's order.
     *
     * @param customer whose appointments must not overlap; null when nobody is booking yet
     */
    public List<MemberId> freeMembers(BookingContext context, CustomerId customer, TimeRange range, BookingSource source) {
        var day = context.calendar().dayOf(LocalDate.ofInstant(range.start(), context.calendar().zone()));
        var performers = context.service().performers();
        return performers.stream()
                .filter(member -> isFree(context, customer, member, range, source))
                .sorted(Comparator.<MemberId>comparingLong(member -> appointmentsOn(context, member, day))
                        .thenComparingInt(performers::indexOf))
                .toList();
    }

    /**
     * B17: the member who takes an appointment when none was chosen: the least busy that day, then the team's order.
     *
     * @throws ServiceNotBookableException when nobody performs the service
     * @throws DomainException the first performer's reason when nobody is free
     */
    public MemberId chooseMember(BookingContext context, CustomerId customer, TimeRange range, BookingSource source) {
        var performers = context.service().performers();
        if (performers.isEmpty()) throw new ServiceNotBookableException("Nobody performs this service");
        var free = freeMembers(context, customer, range, source);
        if (!free.isEmpty()) return free.getFirst();
        throw firstViolation(context, customer, performers.getFirst(), range, source, null);
    }

    private static DomainException firstViolation(BookingContext context, CustomerId customer, MemberId member,
                                                  TimeRange range, BookingSource source, AppointmentId ignoring) {
        if (!context.service().isPerformedBy(member)) return new MemberCannotPerformServiceException(member);
        var start = range.start();
        if (source == BookingSource.ASSISTANT) {
            if (start.isBefore(context.now().plus(ASSISTANT_MIN_LEAD)))
                return new OutsideBookingWindowException("The assistant books at least 30 minutes ahead");
            if (start.isAfter(context.now().plus(ASSISTANT_MAX_AHEAD)))
                return new OutsideBookingWindowException("The assistant books at most 60 days ahead");
        } else if (start.isBefore(context.now().minus(STAFF_MAX_PAST))) {
            return new OutsideBookingWindowException("Staff record appointments at most 12 hours in the past");
        }
        if (!context.calendar().isOpenDuring(range)) return new OutsideOpeningHoursException();
        if (context.blocks().stream().anyMatch(block -> block.blocks(member, range))) return new TimeBlockedException();
        for (Appointment other : context.occupying()) {
            if (other.getId() != null && other.getId().equals(ignoring)) continue;
            if (!other.occupiesTime() || !other.getRange().overlaps(range)) continue;
            if (other.getStaffMemberId().equals(member)) return new SlotUnavailableException("The member is busy then");
            if (other.getCustomerId().equals(customer))
                return new SlotUnavailableException("The customer has another appointment then");
        }
        return null;
    }

    private static long appointmentsOn(BookingContext context, MemberId member, TimeRange day) {
        return context.occupying().stream()
                .filter(appointment -> appointment.occupiesTime() && appointment.getStaffMemberId().equals(member))
                .filter(appointment -> appointment.getRange().overlaps(day))
                .count();
    }
}
