package com.takodu.api.booking.domain.model.aggregates;

import com.takodu.api.booking.domain.exceptions.InvalidAppointmentTransitionException;
import com.takodu.api.booking.domain.exceptions.NoShowBeforeStartException;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.booking.domain.model.valueobjects.CancelReason;
import com.takodu.api.booking.domain.model.valueobjects.CancelledBy;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentTest {
    private static final Instant TEN = Instant.parse("2026-11-02T15:00:00Z");

    private final MemberId ana = new MemberId(UUID.randomUUID());
    private final ServiceOffer haircut = new ServiceOffer(new ServiceId(UUID.randomUUID()),
            new EstablishmentId(UUID.randomUUID()), Duration.ofMinutes(45), Money.pen(3500), true, List.of(ana));

    @Test
    void bookingCopiesThePriceAndTheDuration() {
        var appointment = book();

        assertEquals(Money.pen(3500), appointment.getPrice());
        assertEquals(TEN.plus(Duration.ofMinutes(45)), appointment.getRange().end());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void staffBookingsRecordWhoBooked() {
        assertThrows(IllegalArgumentException.class, () -> Appointment.book(new OrganizationId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()), haircut, ana, TEN, BookingSource.STAFF, null, key()));
    }

    @Test
    void reschedulingKeepsTheLength() {
        var appointment = book();
        var beto = new MemberId(UUID.randomUUID());

        appointment.reschedule(beto, TEN.plus(Duration.ofDays(1)), key());

        assertEquals(beto, appointment.getStaffMemberId());
        assertEquals(Duration.ofMinutes(45), appointment.getRange().length());
    }

    @Test
    void onlyConfirmedAppointmentsAreRescheduled() {
        var appointment = book();
        appointment.changeStatus(AppointmentStatus.IN_PROGRESS, TEN, key());

        assertThrows(InvalidAppointmentTransitionException.class, () -> appointment.reschedule(ana, TEN, key()));
    }

    @Test
    void inProgressAppointmentCanStillBeCancelled() {
        var appointment = book();
        appointment.changeStatus(AppointmentStatus.IN_PROGRESS, TEN, key());

        appointment.cancel(new CancelReason("Se sintió mal"), CancelledBy.CUSTOMER, key());

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertFalse(appointment.occupiesTime());
        assertEquals(CancelledBy.CUSTOMER, appointment.getCancelledBy());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW"})
    void finalAppointmentsDoNotChange(AppointmentStatus last) {
        var appointment = book();
        appointment.changeStatus(last, TEN, key());

        assertThrows(InvalidAppointmentTransitionException.class,
                () -> appointment.cancel(new CancelReason("Tarde"), CancelledBy.STAFF, key()));
        assertThrows(InvalidAppointmentTransitionException.class,
                () -> appointment.changeStatus(AppointmentStatus.IN_PROGRESS, TEN, key()));
    }

    @Test
    void cancelledAppointmentIsFinal() {
        var appointment = book();
        appointment.cancel(new CancelReason("Viaje"), CancelledBy.STAFF, key());

        assertThrows(InvalidAppointmentTransitionException.class,
                () -> appointment.cancel(new CancelReason("Viaje"), CancelledBy.STAFF, key()));
    }

    @Test
    void noShowOnlyAfterTheStart() {
        var appointment = book();

        assertThrows(NoShowBeforeStartException.class,
                () -> appointment.changeStatus(AppointmentStatus.NO_SHOW, TEN.minusSeconds(1), key()));
        appointment.changeStatus(AppointmentStatus.NO_SHOW, TEN, key());
        assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
    }

    @Test
    void inProgressAppointmentCannotBecomeANoShow() {
        var appointment = book();
        appointment.changeStatus(AppointmentStatus.IN_PROGRESS, TEN, key());

        assertThrows(InvalidAppointmentTransitionException.class,
                () -> appointment.changeStatus(AppointmentStatus.NO_SHOW, TEN.plusSeconds(60), key()));
    }

    @Test
    void cancellingNeedsAReason() {
        assertThrows(IllegalArgumentException.class, () -> book().changeStatus(AppointmentStatus.CANCELLED, TEN, key()));
        assertThrows(IllegalArgumentException.class, () -> new CancelReason(" "));
        assertThrows(IllegalArgumentException.class, () -> new CancelReason("x".repeat(501)));
    }

    @Test
    void lastKeyMarksTheTransitionAsApplied() {
        var appointment = book();
        var key = key();

        appointment.changeStatus(AppointmentStatus.COMPLETED, TEN, key);

        assertTrue(appointment.alreadyApplied(key));
        assertFalse(appointment.alreadyApplied(key()));
    }

    @Test
    void customerMayChangeUntilTwoHoursBeforeTheStart() {
        var appointment = book();

        assertTrue(appointment.customerMayChange(TEN.minus(Duration.ofHours(2))));
        assertFalse(appointment.customerMayChange(TEN.minus(Duration.ofHours(2)).plusSeconds(1)));
    }

    @Test
    void customerMayNotChangeAnAppointmentThatStarted() {
        var appointment = book();
        appointment.changeStatus(AppointmentStatus.IN_PROGRESS, TEN.minus(Duration.ofDays(1)), key());

        assertFalse(appointment.customerMayChange(TEN.minus(Duration.ofDays(1))));
    }

    private Appointment book() {
        var booked = Appointment.book(new OrganizationId(UUID.randomUUID()), new CustomerId(UUID.randomUUID()), haircut,
                ana, TEN, BookingSource.STAFF, new UserId(UUID.randomUUID()), key());
        return new Appointment(new AppointmentId(UUID.randomUUID()), booked.getOrganizationId(),
                booked.getEstablishmentId(), booked.getCustomerId(), booked.getServiceId(), booked.getStaffMemberId(),
                booked.getRange(), booked.getPrice(), booked.getStatus(), booked.getSource(), booked.getCreatedBy(), null,
                null, booked.getIdempotencyKey(), null, 0);
    }

    private static IdempotencyKey key() {
        return new IdempotencyKey(UUID.randomUUID());
    }
}
