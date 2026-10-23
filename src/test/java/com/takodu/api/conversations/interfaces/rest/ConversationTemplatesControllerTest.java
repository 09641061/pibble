package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.jobs.OutboundMessageSender;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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

import static com.takodu.api.conversations.TestInbox.conversation;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Staff reach a customer whose window closed with an appointment's template (07 §7). */
@QuarkusTest
class ConversationTemplatesControllerTest {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));

    @Inject
    TestClock clock;

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
    void reminderReachesACustomerWhoseWindowClosed() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        var chat = chatTwoDaysAgo(salon, salon.establishmentId());
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));

        template(staff, chat.conversationId(), UUID.randomUUID(), "reminder", appointment).statusCode(201)
                .body("origin", equalTo("staff"))
                .body("kind", equalTo("template"))
                .body("templateName", equalTo("takodu_reminder"))
                .body("status", equalTo("pending"))
                .body("sentByMemberId", equalTo(staff.memberId().toString()))
                .body("body", startsWith("Hola Customer "));
        sender.sendDue();

        var sent = gateway.sentTo(chat.waId());
        assertEquals(1, sent.size());
        assertTrue(sent.getFirst().startsWith("takodu_reminder(Customer "), sent.getFirst());
        assertTrue(sent.getFirst().endsWith("|Corte|" + establishmentName(salon) + "|"
                + DATE.format(TestSalon.at(clock, 3, "10:00").atZone(TestSalon.LIMA))
                + "|10:00)"), sent.getFirst());
        conversation(staff, chat.conversationId()).body("assistantPaused", equalTo(true));
        messages(staff, chat.conversationId()).body("items[0].status", equalTo("sent"));
    }

    @Test
    void confirmationInsideTheWindowGoesAsText() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = waIdOf(salon);
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));

        template(salon.owner(), conversation, UUID.randomUUID(), "confirmation", appointment).statusCode(201)
                .body("kind", equalTo("text"))
                .body("templateName", equalTo(null));
        sender.sendDue();

        assertTrue(gateway.sentTo(waId).getLast().startsWith("Hola Customer "), gateway.sentTo(waId).getLast());
    }

    @Test
    void repeatedKeyQueuesOneMessage() {
        var salon = TestSalon.open();
        var chat = chatTwoDaysAgo(salon, salon.establishmentId());
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));
        var key = UUID.randomUUID();

        String first = template(salon.owner(), chat.conversationId(), key, "reminder", appointment).statusCode(201)
                .extract().path("id");
        template(salon.owner(), chat.conversationId(), key, "reminder", appointment).statusCode(201)
                .body("id", equalTo(first));
        sender.sendDue();

        assertEquals(1, gateway.sentTo(chat.waId()).size());
        messages(salon.owner(), chat.conversationId()).body("items", hasSize(2));
    }

    @Test
    void refusedWhileThePlanIsInactive() {
        var salon = TestSalon.open();
        var chat = chatTwoDaysAgo(salon, salon.establishmentId());
        var appointment = book(salon, TestSalon.at(clock, 20, "10:00"));
        clock.advance(Duration.ofDays(15));

        template(salon.owner(), chat.conversationId(), UUID.randomUUID(), "reminder", appointment).statusCode(402);
    }

    @Test
    void refusedWhileTheLineIsDisconnected() {
        var salon = TestSalon.open();
        var chat = chatTwoDaysAgo(salon, salon.establishmentId());
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));
        given().spec(salon.owner().spec()).delete("/api/v1/establishments/" + salon.establishmentId() + "/whatsapp-line")
                .then().statusCode(204);

        template(salon.owner(), chat.conversationId(), UUID.randomUUID(), "reminder", appointment).statusCode(422)
                .body("code", equalTo("LINE_DISCONNECTED"));
    }

    @Test
    void refusedWhileMetaHasNotApprovedTheTemplate() {
        var salon = TestSalon.open();
        var wabaId = TestWhatsapp.newPhoneNumberId();
        var line = TestWhatsapp.newPhoneNumberId();
        TestWhatsapp.connect(salon.owner(), salon.establishmentId(), UUID.randomUUID(), line, wabaId).statusCode(201);
        MetaWebhooks.post(MetaWebhooks.payload("template-status", Map.of("WABA_ID", wabaId, "EVENT", "PENDING",
                "TEMPLATE_NAME", "takodu_reminder"))).statusCode(200);
        var waId = waIdOf(salon);
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId,
                clock.instant().minus(Duration.ofDays(2)));
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));

        template(salon.owner(), conversation, UUID.randomUUID(), "reminder", appointment).statusCode(422)
                .body("code", equalTo("TEMPLATE_NOT_APPROVED"));
        sender.sendDue();
        assertEquals(List.of(), gateway.sentTo(waId));
    }

    @Test
    void onlyTheCustomersUpcomingAppointmentAndOnlyReminderOrConfirmation() {
        var salon = TestSalon.open();
        var chat = chatTwoDaysAgo(salon, salon.establishmentId());
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));
        var someoneElses = book(salon, salon.newCustomer(), TestSalon.at(clock, 3, "12:00"));

        template(salon.owner(), chat.conversationId(), UUID.randomUUID(), "reminder", someoneElses).statusCode(422)
                .body("code", equalTo("APPOINTMENT_NOT_UPCOMING"));
        template(salon.owner(), chat.conversationId(), UUID.randomUUID(), "cancellation", appointment).statusCode(400);
        given().spec(salon.owner().spec()).contentType(ContentType.JSON)
                .body(Map.of("template", "reminder", "appointmentId", appointment.toString()))
                .post("/api/v1/conversations/" + chat.conversationId() + "/templates").then().statusCode(400);
    }

    @Test
    void staffOfAnotherEstablishmentAreRefusedAndAnotherOrganizationFindsNothing() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var chat = chatTwoDaysAgo(salon, surco);
        var appointment = book(salon, TestSalon.at(clock, 3, "10:00"));

        template(salon.hireStaff(), chat.conversationId(), UUID.randomUUID(), "reminder", appointment).statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
        template(TestTenants.owner(), chat.conversationId(), UUID.randomUUID(), "reminder", appointment)
                .statusCode(404);
    }

    private record Chat(String waId, UUID conversationId) { }

    /** The salon's customer wrote two days ago, so the window is closed. */
    private Chat chatTwoDaysAgo(TestSalon salon, UUID establishmentId) {
        var line = TestWhatsapp.connectLine(salon.owner(), establishmentId);
        var waId = waIdOf(salon);
        return new Chat(waId, customerWrites(salon.owner(), establishmentId, line, waId,
                clock.instant().minus(Duration.ofDays(2))));
    }

    private static UUID book(TestSalon salon, Instant start) {
        return book(salon, salon.customerId(), start);
    }

    private static UUID book(TestSalon salon, UUID customerId, Instant start) {
        String id = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("customerId", customerId, "serviceId", salon.serviceId(), "start", start.toString()))
                .post("/api/v1/appointments").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    private static ValidatableResponse template(TestTenant tenant, UUID conversationId, UUID key, String template,
                                                UUID appointmentId) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(Map.of("template", template, "appointmentId", appointmentId.toString()))
                .post("/api/v1/conversations/" + conversationId + "/templates").then();
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
}
