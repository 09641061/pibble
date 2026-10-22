package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.takodu.api.conversations.ConversationRows.notificationsOf;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Booking's events, published by the real booking endpoints, schedule the customer's notifications (W11-W13). */
@QuarkusTest
class AppointmentEventHandlersTest {

    @Inject
    TestClock clock;

    @Inject
    Event<AppointmentBookedIntegrationEvent> appointmentBooked;

    @BeforeEach
    void eightInTheMorningInLima() {
        clock.set(TestSalon.at(clock, 1, "08:00"));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffBookingDaysAheadIsConfirmedNowAndRemindedADayBefore() {
        var salon = TestSalon.open();
        var start = TestSalon.at(clock, 3, "10:00");

        var notifications = notificationsOf(book(salon, start));

        assertEquals(Set.of("reminder", "confirmation"), notifications.keySet());
        assertEquals(start.minus(Duration.ofHours(24)), sendAt(notifications.get("reminder")));
        assertEquals(clock.instant(), sendAt(notifications.get("confirmation")));
        assertEquals("pending", notifications.get("reminder").get("status"));
    }

    @Test
    void bookingLessThanADayAheadIsRemindedTwoHoursBefore() {
        var salon = TestSalon.open();
        var start = TestSalon.at(clock, 0, "12:00");

        assertEquals(start.minus(Duration.ofHours(2)), sendAt(notificationsOf(book(salon, start)).get("reminder")));
    }

    @Test
    void bookingLessThanThreeHoursAheadIsOnlyConfirmed() {
        var salon = TestSalon.open();

        assertEquals(Set.of("confirmation"), notificationsOf(book(salon, TestSalon.at(clock, 0, "10:30"))).keySet());
    }

    @Test
    void assistantBookingIsNotConfirmedAndARepeatedEventAddsNothing() {
        var salon = TestSalon.open();
        var start = TestSalon.at(clock, 3, "10:00");
        var appointmentId = book(salon, start);
        QuarkusTransaction.requiringNew().run(() -> Arc.container().instance(EntityManager.class).get()
                .createNativeQuery("delete from scheduled_messages where appointment_id = :id")
                .setParameter("id", appointmentId).executeUpdate());

        for (int i = 0; i < 2; i++) {
            QuarkusTransaction.requiringNew().run(() -> appointmentBooked.fire(new AppointmentBookedIntegrationEvent(
                    salon.owner().organizationId(), appointmentId, salon.establishmentId(), salon.customerId(),
                    salon.owner().memberId(), salon.serviceId(), start, start.plus(Duration.ofHours(1)),
                    "assistant")));
        }

        assertEquals(Set.of("reminder"), notificationsOf(appointmentId).keySet());
    }

    @Test
    void reschedulingMovesTheReminderAndCancelsItWhenTooClose() {
        var salon = TestSalon.open();
        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));
        var later = TestSalon.at(clock, 5, "15:00");

        transition(salon.owner(), appointmentId, "reschedulings", Map.of("start", later.toString())).statusCode(200);
        assertEquals(later.minus(Duration.ofHours(24)), sendAt(notificationsOf(appointmentId).get("reminder")));
        assertEquals(later, toInstant(notificationsOf(appointmentId).get("confirmation").get("starts_at")));

        transition(salon.owner(), appointmentId, "reschedulings",
                Map.of("start", TestSalon.at(clock, 0, "10:00").toString())).statusCode(200);
        assertEquals("cancelled", notificationsOf(appointmentId).get("reminder").get("status"));
    }

    @Test
    void appointmentBookedTooCloseGetsAReminderOnceMovedFarEnough() {
        var salon = TestSalon.open();
        var appointmentId = book(salon, TestSalon.at(clock, 0, "10:00"));
        var later = TestSalon.at(clock, 2, "10:00");

        transition(salon.owner(), appointmentId, "reschedulings", Map.of("start", later.toString())).statusCode(200);

        assertEquals(later.minus(Duration.ofHours(24)), sendAt(notificationsOf(appointmentId).get("reminder")));
    }

    @Test
    void staffCancellationTellsTheCustomerWhy() {
        var salon = TestSalon.open();
        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));

        transition(salon.owner(), appointmentId, "cancellations", Map.of("reason", "Se fue la luz")).statusCode(200);

        var notifications = notificationsOf(appointmentId);
        assertEquals("cancelled", notifications.get("reminder").get("status"));
        assertEquals("cancelled", notifications.get("confirmation").get("status"));
        assertEquals("pending", notifications.get("cancellation").get("status"));
        assertEquals("Se fue la luz", notifications.get("cancellation").get("cancel_reason"));
    }

    @Test
    void customerWhoCancelledIsNotToldAgain() {
        var salon = TestSalon.open();
        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));

        transition(salon.owner(), appointmentId, "cancellations",
                Map.of("reason", "Llamó para cancelar", "cancelledBy", "customer")).statusCode(200);

        assertEquals(Set.of("reminder", "confirmation"), notificationsOf(appointmentId).keySet());
    }

    private static UUID book(TestSalon salon, Instant start) {
        String id = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("customerId", salon.customerId(), "serviceId", salon.serviceId(), "start", start.toString()))
                .post("/api/v1/appointments").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    private static ValidatableResponse transition(TestTenant tenant, UUID appointmentId, String noun,
                                                  Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(body)
                .post("/api/v1/appointments/" + appointmentId + "/" + noun).then();
    }

    private static Instant sendAt(Map<String, Object> row) {
        return toInstant(row.get("send_at"));
    }

    /** Native queries return timestamptz as {@link Instant} or {@link OffsetDateTime}, depending on the driver. */
    private static Instant toInstant(Object value) {
        return value instanceof OffsetDateTime offset ? offset.toInstant() : (Instant) value;
    }
}
