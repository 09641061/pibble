package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.conversations.ConversationRows.notificationsOf;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Appointments booked through the real endpoints reach the customer through the dispatcher and the outbox. */
@QuarkusTest
class ScheduledMessageDispatcherTest {

    @Inject
    TestClock clock;

    @Inject
    ScheduledMessageDispatcher dispatcher;

    @Inject
    OutboundMessageSender sender;

    @Inject
    RecordingWhatsappGateway gateway;

    @BeforeEach
    void eightInTheMorningInLima() {
        clock.set(TestSalon.at(clock, 1, "08:00"));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void confirmationOutsideTheWindowGoesAsTheTemplateAndShowsInTheInbox() {
        var salon = TestSalon.open();
        TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);

        book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();

        var sent = gateway.sentTo(waId);
        assertEquals(1, sent.size());
        assertTrue(sent.getFirst().startsWith("takodu_confirmation(Customer "), sent.getFirst());
        assertTrue(sent.getFirst().endsWith("|Corte|" + establishmentName(salon) + "|" + date(3) + "|10:00)"),
                sent.getFirst());
        messages(salon.owner(), conversationOf(salon, waId)).statusCode(200)
                .body("items[0].origin", equalTo("system"))
                .body("items[0].kind", equalTo("template"))
                .body("items[0].templateName", equalTo("takodu_confirmation"))
                .body("items[0].status", equalTo("sent"))
                .body("items[0].body", startsWith("Hola Customer "));
    }

    @Test
    void confirmationInsideTheWindowGoesAsText() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());

        book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();

        assertEquals(List.of("Hola Customer %s, tu cita de Corte en %s quedó agendada para el %s a las 10:00. ¡Te esperamos!"
                .formatted(waId.substring(2), establishmentName(salon), date(3))), gateway.sentTo(waId));
    }

    @Test
    void reminderGoesAsTheTemplateADayBeforeEvenWithTheWindowOpen() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        var start = TestSalon.at(clock, 3, "10:00");
        var appointmentId = book(salon, start);
        drain();

        clock.set(start.minus(Duration.ofHours(24)).minusSeconds(1));
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        drain();
        assertEquals(1, gateway.sentTo(waId).size());

        clock.advance(Duration.ofSeconds(1));
        drain();
        assertTrue(gateway.sentTo(waId).getLast().startsWith("takodu_reminder("), gateway.sentTo(waId).getLast());
        assertEquals("sent", notificationsOf(appointmentId).get("reminder").get("status"));
    }

    @Test
    void disconnectedLineHoldsNotificationsUntilItIsConnectedAgain() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        disconnect(salon);

        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();
        assertEquals(List.of(), gateway.sentTo(waId));
        assertNotNull(notificationsOf(appointmentId).get("confirmation").get("reason"));

        TestWhatsapp.connect(salon.owner(), salon.establishmentId(), UUID.randomUUID(), line).statusCode(201);
        clock.advance(Duration.ofMinutes(10));
        drain();
        assertEquals(1, gateway.sentTo(waId).size());
    }

    @Test
    void heldNotificationsAreSkippedWithinAnHourOfTheAppointment() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        disconnect(salon);
        var appointmentId = book(salon, TestSalon.at(clock, 0, "12:00"));
        drain();
        clock.set(TestSalon.at(clock, 0, "10:00"));
        drain();

        TestWhatsapp.connect(salon.owner(), salon.establishmentId(), UUID.randomUUID(), line).statusCode(201);
        clock.set(TestSalon.at(clock, 0, "11:00"));
        drain();

        assertEquals(List.of(), gateway.sentTo(waId));
        assertEquals("skipped", notificationsOf(appointmentId).get("confirmation").get("status"));
        assertEquals("skipped", notificationsOf(appointmentId).get("reminder").get("status"));
    }

    @Test
    void templateWaitingForMetasApprovalHoldsTheNotification() {
        var salon = TestSalon.open();
        var wabaId = TestWhatsapp.newPhoneNumberId();
        TestWhatsapp.connect(salon.owner(), salon.establishmentId(), UUID.randomUUID(), TestWhatsapp.newPhoneNumberId(),
                wabaId).statusCode(201);
        var waId = waIdOf(salon);
        templateStatus(wabaId, "takodu_confirmation", "PENDING");

        book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();
        assertEquals(List.of(), gateway.sentTo(waId));

        templateStatus(wabaId, "takodu_confirmation", "APPROVED");
        clock.advance(Duration.ofMinutes(10));
        drain();
        assertEquals(1, gateway.sentTo(waId).size());
    }

    @Test
    void inactivePlanHoldsTheReminder() {
        var salon = TestSalon.open();
        TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        var start = TestSalon.at(clock, 20, "10:00");
        var appointmentId = book(salon, start);
        drain();

        clock.set(start.minus(Duration.ofHours(24)));
        drain();

        assertEquals(1, gateway.sentTo(waId).size());
        var reminder = notificationsOf(appointmentId).get("reminder");
        assertEquals("pending", reminder.get("status"));
        assertEquals("The plan is inactive", reminder.get("reason"));
    }

    @Test
    void staffCancellationTellsTheCustomerWhy() {
        var salon = TestSalon.open();
        TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();

        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("reason", "Se fue la luz"))
                .post("/api/v1/appointments/" + appointmentId + "/cancellations").then().statusCode(200);
        drain();

        assertTrue(gateway.sentTo(waId).getLast().startsWith("takodu_cancellation("));
        assertTrue(gateway.sentTo(waId).getLast().endsWith("|" + date(3) + " a las 10:00|Se fue la luz)"));
    }

    @Test
    void notificationMetaRefusesIsShownFailedInTheInbox() {
        var salon = TestSalon.open();
        TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        gateway.failFor(waId, 1, "131026: Message undeliverable", false);

        var appointmentId = book(salon, TestSalon.at(clock, 3, "10:00"));
        drain();

        assertEquals("sent", notificationsOf(appointmentId).get("confirmation").get("status"));
        messages(salon.owner(), conversationOf(salon, waId))
                .body("items[0].status", equalTo("failed"))
                .body("items[0].error", equalTo("131026: Message undeliverable"));
    }

    /** Other tests leave due rows too, so each job runs a few rounds. */
    private void drain() {
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatchDue();
            sender.sendDue();
        }
    }

    private static UUID book(TestSalon salon, Instant start) {
        String id = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("customerId", salon.customerId(), "serviceId", salon.serviceId(), "start", start.toString()))
                .post("/api/v1/appointments").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    private static void disconnect(TestSalon salon) {
        given().spec(salon.owner().spec()).delete("/api/v1/establishments/" + salon.establishmentId() + "/whatsapp-line")
                .then().statusCode(204);
    }

    private static void templateStatus(String wabaId, String template, String event) {
        MetaWebhooks.post(MetaWebhooks.payload("template-status", Map.of("WABA_ID", wabaId, "EVENT", event,
                "TEMPLATE_NAME", template))).statusCode(200);
    }

    private static String waIdOf(TestSalon salon) {
        String phone = given().spec(salon.owner().spec()).get("/api/v1/customers/" + salon.customerId())
                .then().statusCode(200).extract().path("phone");
        return phone.substring(1);
    }

    private static String establishmentName(TestSalon salon) {
        return given().spec(salon.owner().spec()).get("/api/v1/establishments/" + salon.establishmentId())
                .then().statusCode(200).extract().path("name");
    }

    private static UUID conversationOf(TestSalon salon, String waId) {
        String id = given().spec(salon.owner().spec())
                .get("/api/v1/establishments/" + salon.establishmentId() + "/conversations")
                .then().statusCode(200)
                .extract().path("items.find { it.customerPhone == '+%s' }.id".formatted(waId));
        return UUID.fromString(id);
    }

    private String date(int daysAhead) {
        return DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"))
                .format(TestSalon.day(clock, daysAhead));
    }
}
