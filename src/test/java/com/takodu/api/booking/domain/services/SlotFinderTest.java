package com.takodu.api.booking.domain.services;

import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.EstablishmentCalendar;
import com.takodu.api.booking.domain.model.valueobjects.OpeningWindow;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.booking.domain.model.valueobjects.Slot;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotFinderTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final Instant SUNDAY = MONDAY.minusDays(1).atStartOfDay(LIMA).toInstant();

    private final SlotFinder finder = new SlotFinder(new AvailabilityPolicy());
    private final EstablishmentId branch = new EstablishmentId(UUID.randomUUID());
    private final MemberId ana = new MemberId(UUID.randomUUID());
    private final MemberId beto = new MemberId(UUID.randomUUID());
    private final CustomerId rosa = new CustomerId(UUID.randomUUID());
    private final EstablishmentCalendar calendar = new EstablishmentCalendar(branch, LIMA, List.of(
            new OpeningWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
            new OpeningWindow(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(16, 0))), false, true);
    private final ServiceOffer haircut = new ServiceOffer(new ServiceId(UUID.randomUUID()), branch, Duration.ofHours(1),
            Money.pen(3500), true, List.of(ana, beto));

    @Test
    void slotsStartEvery15MinutesAndFitTheirWindow() {
        var starts = find(List.of(), SUNDAY, null).stream().map(slot -> local(slot.range().start())).toList();

        assertEquals(List.of("09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30", "10:45", "11:00", "15:00"),
                starts);
    }

    @Test
    void slotListsEveryFreeMemberInTheTeamOrder() {
        assertEquals(List.of(ana, beto), find(List.of(), SUNDAY, null).getFirst().freeMembers());
    }

    @Test
    void busyMemberLeavesTheSlotsOfTheOthers() {
        var slots = find(List.of(appointment(ana, new CustomerId(UUID.randomUUID()), "10:00")), SUNDAY, null);

        var atTen = slots.stream().filter(slot -> local(slot.range().start()).equals("10:00")).findFirst().orElseThrow();
        assertEquals(List.of(beto), atTen.freeMembers());
        assertEquals(List.of(beto, ana), slots.getFirst().freeMembers());
    }

    @Test
    void askingForOneMemberShowsOnlyTheirSlots() {
        var context = new BookingContext(calendar, haircut, List.of(),
                List.of(appointment(ana, new CustomerId(UUID.randomUUID()), "10:00")), SUNDAY);

        var starts = finder.find(context, MONDAY, null, ana, BookingSource.STAFF).stream()
                .map(slot -> local(slot.range().start())).toList();

        assertFalse(starts.contains("10:30"));
        assertTrue(starts.contains("11:00"));
    }

    @Test
    void busyCustomerHasNoSlotThen() {
        var starts = find(List.of(appointment(beto, rosa, "10:00")), SUNDAY, rosa).stream()
                .map(slot -> local(slot.range().start())).toList();

        assertFalse(starts.contains("10:30"));
        assertTrue(starts.contains("11:00"));
    }

    @Test
    void pastStartsAreLeftOut() {
        var tenFifteen = MONDAY.atTime(10, 15).atZone(LIMA).toInstant();

        assertEquals("10:15", local(find(List.of(), tenFifteen, null).getFirst().range().start()));
    }

    @Test
    void closedDayHasNoSlots() {
        var context = new BookingContext(calendar, haircut, List.of(), List.of(), SUNDAY);

        assertTrue(finder.find(context, MONDAY.plusDays(1), null, null, BookingSource.STAFF).isEmpty());
    }

    private List<Slot> find(List<Appointment> occupying, Instant now, CustomerId customer) {
        return finder.find(new BookingContext(calendar, haircut, List.of(), occupying, now), MONDAY, customer, null,
                BookingSource.STAFF);
    }

    private Appointment appointment(MemberId member, CustomerId customer, String start) {
        return new Appointment(new AppointmentId(UUID.randomUUID()), new OrganizationId(UUID.randomUUID()), branch,
                customer, haircut.id(), member,
                TimeRange.of(MONDAY.atTime(LocalTime.parse(start)).atZone(LIMA).toInstant(), Duration.ofHours(1)),
                haircut.price(), AppointmentStatus.CONFIRMED, BookingSource.STAFF, new UserId(UUID.randomUUID()), null,
                null, null, null, 0);
    }

    private static String local(Instant instant) {
        return LocalTime.ofInstant(instant, LIMA).toString();
    }
}
