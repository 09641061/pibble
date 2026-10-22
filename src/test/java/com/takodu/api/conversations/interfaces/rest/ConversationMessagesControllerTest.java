package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.jobs.OutboundMessageSender;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.conversation;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static com.takodu.api.conversations.TestInbox.reply;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ConversationMessagesControllerTest {

    @Inject
    TestClock clock;

    @Inject
    OutboundMessageSender sender;

    @Inject
    RecordingWhatsappGateway gateway;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffReplyIsQueuedThenSentAndPausesTheAssistant() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(staff, salon.establishmentId(), line, waId, Instant.now());

        reply(staff, conversation, UUID.randomUUID(), "Sí, mañana a las 4 está libre").statusCode(201)
                .body("status", equalTo("pending"))
                .body("origin", equalTo("staff"))
                .body("sentByMemberId", equalTo(staff.memberId().toString()));
        conversation(staff, conversation).body("unreadCount", equalTo(0)).body("assistantPaused", equalTo(true));

        sender.sendDue();

        assertEquals(List.of("Sí, mañana a las 4 está libre"), gateway.sentTo(waId));
        messages(staff, conversation).statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].status", equalTo("sent"))
                .body("items[1].origin", equalTo("customer"));
    }

    @Test
    void secondTapWithTheSameKeySendsOnce() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        var key = UUID.randomUUID();
        String id = reply(salon.owner(), conversation, key, "Hola").statusCode(201).extract().path("id");

        reply(salon.owner(), conversation, key, "Hola").statusCode(201).body("id", equalTo(id));
        sender.sendDue();

        assertEquals(1, gateway.sentTo(waId).size());
    }

    @Test
    void sendersRunningAtOnceSendEachMessageOnce() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        for (int i = 0; i < 3; i++) reply(salon.owner(), conversation, UUID.randomUUID(), "Mensaje " + i).statusCode(201);

        IntStream.range(0, 4).mapToObj(i -> CompletableFuture.runAsync(sender::sendDue)).toList()
                .forEach(CompletableFuture::join);

        assertEquals(3, gateway.sentTo(waId).size());
    }

    @Test
    void outsideTheWindowOnlyTemplatesMayBeSent() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(),
                Instant.now().minus(Duration.ofHours(24)).minusSeconds(5));

        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(422)
                .body("code", equalTo("WINDOW_CLOSED"));
    }

    @Test
    void inactivePlanOrDisconnectedLineSendsNothing() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());

        given().spec(salon.owner().spec()).delete("/api/v1/establishments/" + salon.establishmentId() + "/whatsapp-line")
                .then().statusCode(204);
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(422)
                .body("code", equalTo("LINE_DISCONNECTED"));

        clock.advance(Duration.ofDays(15));
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(402);
        messages(salon.owner(), conversation).body("items", hasSize(1));
    }

    @Test
    void lineDisconnectedBeforeSendingFailsTheMessage() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(201);
        given().spec(salon.owner().spec()).delete("/api/v1/establishments/" + salon.establishmentId() + "/whatsapp-line")
                .then().statusCode(204);

        sender.sendDue();

        assertEquals(0, gateway.sentTo(waId).size());
        messages(salon.owner(), conversation).body("items[0].status", equalTo("failed"))
                .body("items[0].error", notNullValue());
    }

    @Test
    void failedSendsAreRetriedThenShownFailed() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        gateway.failFor(waId, 5, "Meta did not answer", true);
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(201);

        for (int attempt = 0; attempt < 5; attempt++) {
            sender.sendDue();
            messages(salon.owner(), conversation).body("items[0].status", equalTo(attempt < 4 ? "pending" : "failed"));
            clock.advance(Duration.ofMinutes(5));
        }

        messages(salon.owner(), conversation).body("items[0].error", equalTo("Meta did not answer"));
        assertEquals(0, gateway.sentTo(waId).size());
    }

    @Test
    void retryThatSucceedsSendsOnce() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        gateway.failFor(waId, 1, "Meta did not answer", true);
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(201);

        sender.sendDue();
        sender.sendDue();
        assertEquals(0, gateway.sentTo(waId).size(), "the retry waits 30 seconds");
        clock.advance(Duration.ofSeconds(31));
        sender.sendDue();

        assertEquals(1, gateway.sentTo(waId).size());
        messages(salon.owner(), conversation).body("items[0].status", equalTo("sent"));
    }

    @Test
    void refusalMetaWouldRepeatFailsAtOnce() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now());
        gateway.failFor(waId, 1, "131026: Message undeliverable", false);
        reply(salon.owner(), conversation, UUID.randomUUID(), "Hola").statusCode(201);

        sender.sendDue();

        messages(salon.owner(), conversation).body("items[0].status", equalTo("failed"))
                .body("items[0].error", equalTo("131026: Message undeliverable"));
    }

    @Test
    void chatIsPagedNewestFirst() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = newWaId();
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now().minusSeconds(30));
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now().minusSeconds(20));
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, Instant.now().minusSeconds(10));

        String cursor = given().spec(salon.owner().spec()).queryParam("size", 2)
                .get("/api/v1/conversations/" + conversation + "/messages")
                .then().body("items", hasSize(2)).extract().path("nextCursor");
        given().spec(salon.owner().spec()).queryParam("size", 2).queryParam("before", cursor)
                .get("/api/v1/conversations/" + conversation + "/messages")
                .then().body("items", hasSize(1)).body("nextCursor", nullValue());
    }

    @Test
    void replyMustBeTextOfAtMost4096CharactersWithAKey() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());

        reply(salon.owner(), conversation, UUID.randomUUID(), " ").statusCode(400);
        reply(salon.owner(), conversation, UUID.randomUUID(), "a".repeat(4097)).statusCode(400);
        given().spec(salon.owner().spec()).contentType(ContentType.JSON).body(Map.of("body", "Hola"))
                .post("/api/v1/conversations/" + conversation + "/messages").then().statusCode(400);
    }

    @Test
    void staffOfAnotherEstablishmentAndOtherOrganizationsCannotReadOrReply() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var line = TestWhatsapp.connectLine(salon.owner(), surco);
        var conversation = customerWrites(salon.owner(), surco, line, newWaId(), Instant.now());
        var staff = salon.hireStaff();
        var stranger = TestTenants.owner();

        messages(staff, conversation).statusCode(403);
        reply(staff, conversation, UUID.randomUUID(), "Hola").statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        messages(stranger, conversation).statusCode(404).body("code", equalTo("CONVERSATION_NOT_FOUND"));
        reply(stranger, conversation, UUID.randomUUID(), "Hola").statusCode(404);
    }
}
