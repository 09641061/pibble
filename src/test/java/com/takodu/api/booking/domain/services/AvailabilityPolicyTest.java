package com.takodu.api.booking.domain.services;

import com.takodu.api.booking.domain.exceptions.MemberCannotPerformServiceException;
import com.takodu.api.booking.domain.exceptions.OutsideBookingWindowException;
import com.takodu.api.booking.domain.exceptions.OutsideOpeningHoursException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.exceptions.TimeBlockedException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.EstablishmentCalendar;
import com.takodu.api.booking.domain.model.valueobjects.OpeningWindow;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvailabilityPolicyTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final Instant NOW = MONDAY.atTime(8, 0).atZone(LIMA).toInstant();

    private final AvailabilityPolicy policy = new AvailabilityPolicy();
    private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
    private final EstablishmentId branch = new EstablishmentId(UUID.randomUUID());
    private final MemberId ana = new MemberId(UUID.randomUUID());
    private final MemberId beto = new MemberId(UUID.randomUUID());
    private final CustomerId rosa = new CustomerId(UUID.randomUUID());
    private final CustomerId luis = new CustomerId(UUID.randomUUID());
    private final EstablishmentCalendar calendar = new EstablishmentCalendar(branch, LIMA,
            List.of(new OpeningWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))), false, true);
    private final ServiceOffer haircut = new ServiceOffer(new ServiceId(UUID.randomUUID()), branch, Duration.ofHours(1),
            Money.pen(3500), true, List.of(ana, beto));

    @Test
    void freeMemberInsideOpeningHoursIsBookable() {
        assertDoesNotThrow(() -> policy.ensureBookable(context(List.of()), rosa, ana, at("10:00"), BookingSource.STAFF, null));
    }

    @Test
    void memberWhoDoesNotPerformTheServiceIsRefused() {
        var stranger = new MemberId(UUID.randomUUID());

        assertThrows(MemberCannotPerformServiceException.class,
                () -> policy.ensureBookable(context(List.of()), rosa, stranger, at("10:00"), BookingSource.STAFF, null));
    }

    @Test
    void appointmentEndingAfterClosingIsRefused() {
        assertThrows(OutsideOpeningHoursException.class,
                () -> policy.ensureBookable(context(List.of()), rosa, ana, at("17:30"), BookingSource.STAFF, null));
    }

    @Test
    void blockOfTheMemberOrOfEveryoneIsRefused() {
        var lunch = TimeBlock.create(organization, branch, null, at("13:00"), "Almuerzo", key());
        var context = new BookingContext(calendar, haircut, List.of(lunch), List.of(), NOW);

        assertThrows(TimeBlockedException.class,
                () -> policy.ensureBookable(context, rosa, beto, at("12:30"), BookingSource.STAFF, null));
    }

    @Test
    void busyMemberIsRefused() {
        var context = context(List.of(appointment(ana, luis, "10:30", AppointmentStatus.CONFIRMED)));

        var refused = assertThrows(SlotUnavailableException.class,
                () -> policy.ensureBookable(context, rosa, ana, at("10:00"), BookingSource.STAFF, null));
        assertEquals("SLOT_UNAVAILABLE", refused.code());
    }

    @Test
    void customerBusyWithAnotherMemberIsRefused() {
        var context = context(List.of(appointment(beto, rosa, "10:00", AppointmentStatus.IN_PROGRESS)));

        assertThrows(SlotUnavailableException.class,
                () -> policy.ensureBookable(context, rosa, ana, at("10:00"), BookingSource.STAFF, null));
    }

    @Test
    void cancelledAppointmentFreesItsTime() {
        var context = context(List.of(appointment(ana, luis, "10:00", AppointmentStatus.CANCELLED)));

        assertDoesNotThrow(() -> policy.ensureBookable(context, rosa, ana, at("10:00"), BookingSource.STAFF, null));
    }

    @Test
    void rescheduledAppointmentDoesNotCollideWithItself() {
        var own = appointment(ana, rosa, "10:00", AppointmentStatus.CONFIRMED);

        assertDoesNotThrow(() -> policy.ensureBookable(context(List.of(own)), rosa, ana, at("10:30"),
                BookingSource.STAFF, own.getId()));
    }

    @Test
    void staffRecordWalkInsUpTo12HoursBack() {
        var evening = new BookingContext(calendar, haircut, List.of(), List.of(), MONDAY.atTime(22, 0).atZone(LIMA).toInstant());

        assertDoesNotThrow(() -> policy.ensureBookable(evening, rosa, ana, at("10:00"), BookingSource.STAFF, null));
        assertThrows(OutsideBookingWindowException.class,
                () -> policy.ensureBookable(evening, rosa, ana, at("09:00"), BookingSource.STAFF, null));
    }

    @Test
    void assistantBooksAtLeast30MinutesAhead() {
        var context = new BookingContext(calendar, haircut, List.of(), List.of(), MONDAY.atTime(9, 45).atZone(LIMA).toInstant());

        assertThrows(OutsideBookingWindowException.class,
                () -> policy.ensureBookable(context, rosa, ana, at("10:00"), BookingSource.ASSISTANT, null));
        assertDoesNotThrow(() -> policy.ensureBookable(context, rosa, ana, at("10:15"), BookingSource.ASSISTANT, null));
    }

    @Test
    void noChoiceGoesToTheLeastBusyMemberThatDay() {
        var context = context(List.of(appointment(ana, luis, "15:00", AppointmentStatus.CONFIRMED)));

        assertEquals(beto, policy.chooseMember(context, rosa, at("10:00"), BookingSource.STAFF));
    }

    @Test
    void tieGoesToTheFirstMemberInTheTeamOrder() {
        assertEquals(ana, policy.chooseMember(context(List.of()), rosa, at("10:00"), BookingSource.STAFF));
    }

    @Test
    void noChoiceSkipsABusyMember() {
        var context = context(List.of(appointment(ana, luis, "10:00", AppointmentStatus.CONFIRMED)));

        assertEquals(beto, policy.chooseMember(context, rosa, at("10:00"), BookingSource.STAFF));
    }

    @Test
    void noChoiceExplainsWhyNobodyIsFree() {
        assertThrows(OutsideOpeningHoursException.class,
                () -> policy.chooseMember(context(List.of()), rosa, at("18:00"), BookingSource.STAFF));
    }

    @Test
    void serviceNobodyPerformsCannotBeBooked() {
        var nobody = new ServiceOffer(haircut.id(), branch, Duration.ofHours(1), Money.pen(3500), true, List.of());
        var context = new BookingContext(calendar, nobody, List.of(), List.of(), NOW);

        assertThrows(ServiceNotBookableException.class,
                () -> policy.chooseMember(context, rosa, at("10:00"), BookingSource.STAFF));
    }

    private BookingContext context(List<Appointment> occupying) {
        return new BookingContext(calendar, haircut, List.of(), occupying, NOW);
    }

    private Appointment appointment(MemberId member, CustomerId customer, String start, AppointmentStatus status) {
        return new Appointment(new AppointmentId(UUID.randomUUID()), organization, branch, customer, haircut.id(), member,
                at(start), haircut.price(), status, BookingSource.STAFF, new UserId(UUID.randomUUID()), null, null,
                null, null, 0, null);
    }

    private static TimeRange at(String localTime) {
        return TimeRange.of(MONDAY.atTime(LocalTime.parse(localTime)).atZone(LIMA).toInstant(), Duration.ofHours(1));
    }

    private static IdempotencyKey key() {
        return new IdempotencyKey(UUID.randomUUID());
    }
}
