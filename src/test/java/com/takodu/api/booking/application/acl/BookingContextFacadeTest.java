package com.takodu.api.booking.application.acl;

import com.takodu.api.booking.interfaces.acl.BookingContextFacade;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The assistant books through the facade with the same rules as staff, plus B9 and B14. */
@QuarkusTest
class BookingContextFacadeTest {

    @Inject
    TestClock clock;

    @Inject
    BookingContextFacade booking;

    @BeforeEach
    void eightInTheMorningInLima() {
        clock.set(TestSalon.at(clock, 1, "08:00"));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void assistantBookingIsMarkedAsTheAssistantsAndARetryBooksOnce() {
        var salon = TestSalon.open();
        var key = UUID.randomUUID();
        var start = TestSalon.at(clock, 2, "10:00");

        var first = asAssistant(salon, salon.establishmentId(),
                () -> booking.book(org(salon), salon.customerId(), salon.serviceId(), null, start, key));
        var retry = asAssistant(salon, salon.establishmentId(),
                () -> booking.book(org(salon), salon.customerId(), salon.serviceId(), null, start, key));

        assertEquals(first.id(), retry.id());
        assertEquals(salon.owner().memberId(), first.staffMemberId());
        given().spec(salon.owner().spec()).get("/api/v1/appointments/" + first.id())
                .then().statusCode(200).body("source", equalTo("assistant"));
    }

    @Test
    void assistantCannotBookTwentyMinutesAhead() {
        var salon = TestSalon.open();
        clock.set(TestSalon.at(clock, 1, "09:40"));

        var refused = assertThrows(BusinessRuleViolationException.class, () -> asAssistant(salon,
                salon.establishmentId(), () -> booking.book(org(salon), salon.customerId(), salon.serviceId(), null,
                        TestSalon.at(clock, 0, "10:00"), UUID.randomUUID())));

        assertEquals("OUTSIDE_BOOKING_WINDOW", refused.code());
    }

    @Test
    void assistantSlotsStartAtLeastThirtyMinutesFromNow() {
        var salon = TestSalon.open();
        clock.set(TestSalon.at(clock, 1, "09:40"));

        var slots = asAssistant(salon, salon.establishmentId(), () -> booking.fetchSlots(org(salon),
                salon.serviceId(), salon.customerId(), TestSalon.day(clock, 0), null));

        assertEquals(TestSalon.at(clock, 0, "10:15"), slots.getFirst().start());
    }

    @Test
    void customerCancelsTheirOwnAppointmentUntilTwoHoursBefore() {
        var salon = TestSalon.open();
        var appointment = asAssistant(salon, salon.establishmentId(), () -> booking.book(org(salon),
                salon.customerId(), salon.serviceId(), null, TestSalon.at(clock, 0, "12:00"), UUID.randomUUID()));
        assertTrue(appointment.customerMayChange());

        clock.set(TestSalon.at(clock, 0, "10:01"));
        var late = assertThrows(BusinessRuleViolationException.class, () -> asAssistant(salon, salon.establishmentId(),
                () -> booking.cancel(org(salon), salon.customerId(), appointment.id(), "No puedo", UUID.randomUUID())));
        assertEquals("CUSTOMER_CHANGE_TOO_LATE", late.code());

        clock.set(TestSalon.at(clock, 0, "09:00"));
        var cancelled = asAssistant(salon, salon.establishmentId(), () -> booking.cancel(org(salon),
                salon.customerId(), appointment.id(), "No puedo", UUID.randomUUID()));
        assertEquals("cancelled", cancelled.status());
        given().spec(salon.owner().spec()).get("/api/v1/appointments/" + appointment.id())
                .then().statusCode(200).body("cancelledBy", equalTo("customer"));
    }

    @Test
    void customerSeesAndChangesOnlyTheirOwnAppointments() {
        var salon = TestSalon.open();
        var other = salon.newCustomer();
        var start = TestSalon.at(clock, 2, "10:00");
        var theirs = asAssistant(salon, salon.establishmentId(),
                () -> booking.book(org(salon), other, salon.serviceId(), null, start, UUID.randomUUID()));

        var mine = asAssistant(salon, salon.establishmentId(),
                () -> booking.fetchUpcomingAppointments(org(salon), salon.establishmentId(), salon.customerId()));
        assertEquals(List.of(), mine);
        assertThrows(ResourceNotFoundException.class, () -> asAssistant(salon, salon.establishmentId(),
                () -> booking.reschedule(org(salon), salon.customerId(), theirs.id(), start.plus(Duration.ofHours(2)),
                        UUID.randomUUID())));
    }

    @Test
    void assistantOfOneEstablishmentCannotBookAtAnother() {
        var salon = TestSalon.open();
        var second = salon.openSecondEstablishment();

        assertThrows(AccessDeniedException.class, () -> asAssistant(salon, second, () -> booking.book(org(salon),
                salon.customerId(), salon.serviceId(), null, TestSalon.at(clock, 2, "10:00"), UUID.randomUUID())));
    }

    @Test
    void reschedulingKeepsTheMemberAndReportsWhetherItMayStillChange() {
        var salon = TestSalon.open();
        var appointment = asAssistant(salon, salon.establishmentId(), () -> booking.book(org(salon),
                salon.customerId(), salon.serviceId(), null, TestSalon.at(clock, 2, "10:00"), UUID.randomUUID()));
        Instant later = TestSalon.at(clock, 0, "09:30");

        var moved = asAssistant(salon, salon.establishmentId(), () -> booking.reschedule(org(salon),
                salon.customerId(), appointment.id(), later, UUID.randomUUID()));

        assertEquals(later, moved.start());
        assertEquals(appointment.staffMemberId(), moved.staffMemberId());
        assertFalse(moved.customerMayChange());
    }

    private static UUID org(TestSalon salon) {
        return salon.owner().organizationId();
    }

    /** Runs the call the way a turn does: in its own request context, with the assistant bound. */
    private static <T> T asAssistant(TestSalon salon, UUID establishmentId, Supplier<T> call) {
        var requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            Arc.container().instance(TenantContext.class).get().bindAssistant(
                    new OrganizationId(salon.owner().organizationId()), new EstablishmentId(establishmentId));
            return call.get();
        } finally {
            requestContext.terminate();
        }
    }
}
