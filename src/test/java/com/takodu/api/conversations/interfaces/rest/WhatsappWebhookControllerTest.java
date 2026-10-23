package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.RecordingConversationEvents;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.takodu.api.conversations.ConversationRows.conversationsOfLine;
import static com.takodu.api.conversations.ConversationRows.lineStatus;
import static com.takodu.api.conversations.ConversationRows.messagesWithWamid;
import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.MetaWebhooks.newWamid;
import static com.takodu.api.conversations.MetaWebhooks.payload;
import static com.takodu.api.conversations.MetaWebhooks.post;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WhatsappWebhookControllerTest {

    @Inject
    RecordingConversationEvents events;

    @Test
    void customerMessageIsStoredWithItsCustomerAndConversation() {
        var line = connectedLine();
        var waId = newWaId();
        var wamid = newWamid();

        post(payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", wamid))).statusCode(200);

        var message = messagesWithWamid(wamid).getFirst();
        assertEquals("customer", message.get("origin"));
        assertEquals("in", message.get("direction"));
        assertEquals("received", message.get("status"));
        assertEquals("Hola, ¿tienen cita mañana a las 4?", message.get("body"));
        var conversation = conversationsOfLine(line).getFirst();
        assertEquals("+" + waId, conversation.get("phone_e164"));
        assertEquals("Rosa Quispe", conversation.get("whatsapp_profile_name"));
        assertEquals(1, conversation.get("unread_count"));
        assertNotNull(conversation.get("last_inbound_at"));
        assertEquals(1, events.about((UUID) conversation.get("id")).size());
        assertEquals("text", events.about((UUID) conversation.get("id")).getFirst().kind());
    }

    @Test
    void redeliveredMessageIsStoredOnceAndPublishedOnce() {
        var line = connectedLine();
        var body = payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", newWamid()));

        post(body).statusCode(200);
        post(body).statusCode(200);

        var conversation = conversationsOfLine(line).getFirst();
        assertEquals(1, conversation.get("unread_count"));
        assertEquals(1, events.about((UUID) conversation.get("id")).size());
    }

    @Test
    void simultaneousDeliveriesOfANewCustomerStoreOneMessageInOneConversation() {
        var line = connectedLine();
        var wamid = newWamid();
        var body = payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", wamid));

        var statuses = IntStream.range(0, 6)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> post(body).extract().statusCode()))
                .toList().stream().map(CompletableFuture::join).toList();

        assertTrue(statuses.stream().allMatch(status -> status == 200), statuses::toString);
        assertEquals(1, messagesWithWamid(wamid).size());
        assertEquals(1, conversationsOfLine(line).size());
        assertEquals(1, conversationsOfLine(line).getFirst().get("unread_count"));
    }

    @Test
    void secondMessageOfTheCustomerJoinsTheConversation() {
        var line = connectedLine();
        var waId = newWaId();

        post(payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", newWamid())));
        post(payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", newWamid())));

        assertEquals(1, conversationsOfLine(line).size());
        assertEquals(2, conversationsOfLine(line).getFirst().get("unread_count"));
    }

    @Test
    void everyKindIsKeptAndUnknownKindsAreNotDropped() {
        var line = connectedLine();
        var wamid = newWamid();

        post(payload("mixed-messages", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", wamid)))
                .statusCode(200);

        var messages = messagesWithWamid(wamid);
        assertEquals(5, messages.size(), "the message from a malformed sender is skipped");
        var byKind = messages.stream().collect(Collectors.toMap(m -> (String) m.get("kind"), m -> m));
        assertEquals("1003383421387256", byKind.get("image").get("media_id"));
        assertEquals("image/jpeg", byKind.get("image").get("media_mime"));
        assertEquals("Este corte", byKind.get("image").get("body"));
        assertEquals("Sáb 10:00", byKind.get("interactive").get("body"));
        assertEquals("Confirmar", byKind.get("button").get("body"));
        assertEquals("Plaza Mayor (-12.0464,-77.0428)", byKind.get("location").get("body"));
        assertNull(byKind.get("unsupported").get("body"));
    }

    @Test
    void tapOnAButtonOrListRowIsStoredWithItsTitle() {
        var line = connectedLine();
        var waId = newWaId();
        var button = newWamid();
        var row = newWamid();

        post(payload("button-reply", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", button, "BODY", "10:00")))
                .statusCode(200);
        post(payload("list-reply", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", row, "BODY", "Manicure")))
                .statusCode(200);

        assertEquals("interactive", messagesWithWamid(button).getFirst().get("kind"));
        assertEquals("10:00", messagesWithWamid(button).getFirst().get("body"));
        assertEquals("interactive", messagesWithWamid(row).getFirst().get("kind"));
        assertEquals("Manicure", messagesWithWamid(row).getFirst().get("body"));
    }

    @Test
    void ownerReplyFromThePhoneIsStoredAndPausesTheAssistant() {
        var line = connectedLine();
        var waId = newWaId();
        post(payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", newWamid())));
        var echo = newWamid();

        post(payload("business-app-echo", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", echo)))
                .statusCode(200);

        var message = messagesWithWamid(echo).getFirst();
        assertEquals("business_app", message.get("origin"));
        assertEquals("out", message.get("direction"));
        assertEquals("sent", message.get("status"));
        var conversation = conversationsOfLine(line).getFirst();
        assertEquals(0, conversation.get("unread_count"));
        var pausedUntil = (Instant) conversation.get("bot_paused_until");
        assertTrue(pausedUntil.isAfter(Instant.now().plus(11, ChronoUnit.HOURS)));
        assertEquals(1, events.about((UUID) conversation.get("id")).size(), "the echo publishes nothing");
    }

    @Test
    void historyFillsTheChatWithoutUnreadWindowOrAssistant() {
        var line = connectedLine();
        var wamid = newWamid();
        var weekAgo = String.valueOf(Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond());

        post(payload("history", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", wamid,
                "TIMESTAMP", weekAgo))).statusCode(200);

        var messages = messagesWithWamid(wamid);
        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(m -> "history".equals(m.get("origin"))));
        assertEquals("in", messagesWithWamid(wamid + ".in").getFirst().get("direction"));
        assertEquals("read", messagesWithWamid(wamid + ".out").getFirst().get("status"));
        assertEquals("unsupported", messagesWithWamid(wamid + ".media").getFirst().get("kind"));
        var conversation = conversationsOfLine(line).getFirst();
        assertEquals(0, conversation.get("unread_count"));
        assertNull(conversation.get("last_inbound_at"));
        assertTrue(events.about((UUID) conversation.get("id")).isEmpty());
    }

    @Test
    void statusesOnlyMoveForward() {
        var line = connectedLine();
        var waId = newWaId();
        var wamid = newWamid();
        post(payload("business-app-echo", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", wamid)));

        post(status(line, waId, wamid, "read", "[]"));
        post(status(line, waId, wamid, "delivered", "[]"));
        post(status(line, waId, wamid, "failed", "[{\"code\": 131047, \"title\": \"Re-engagement message\"}]"));

        var message = messagesWithWamid(wamid).getFirst();
        assertEquals("read", message.get("status"));
        assertEquals("service", message.get("pricing_category"));
        assertEquals(false, message.get("billable"));
        assertNull(message.get("error"));
    }

    @Test
    void failedStatusKeepsMetasError() {
        var line = connectedLine();
        var waId = newWaId();
        var wamid = newWamid();
        post(payload("business-app-echo", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", wamid)));

        post(status(line, waId, wamid, "failed", "[{\"code\": 131047, \"title\": \"Re-engagement message\"}]"));

        var message = messagesWithWamid(wamid).getFirst();
        assertEquals("failed", message.get("status"));
        assertEquals("131047: Re-engagement message", message.get("error"));
    }

    @Test
    void statusOfAnUnknownMessageIsIgnored() {
        var line = connectedLine();

        post(status(line, newWaId(), newWamid(), "delivered", "[]")).statusCode(200);
    }

    @Test
    void badOrMissingSignatureStoresNothing() {
        var line = connectedLine();
        var wamid = newWamid();
        var body = payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", wamid))
                .getBytes(StandardCharsets.UTF_8);

        post(body, "sha256=" + "0".repeat(64)).statusCode(401).body("code", equalTo("WEBHOOK_SIGNATURE_INVALID"));
        post(body, null).statusCode(401);
        post(body, "sha1=abc").statusCode(401);

        assertTrue(messagesWithWamid(wamid).isEmpty());
    }

    @Test
    void unknownOrDisconnectedLinesAreAcknowledgedAndIgnored() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var line = TestWhatsapp.connectLine(owner, establishment);
        given().spec(owner.spec()).delete("/api/v1/establishments/" + establishment + "/whatsapp-line")
                .then().statusCode(204);
        var wamid = newWamid();

        post(payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", newWaId(), "WAMID", wamid)))
                .statusCode(200);
        post(payload("text-message", Map.of("PHONE_NUMBER_ID", TestWhatsapp.newPhoneNumberId(), "WA_ID", newWaId(),
                "WAMID", wamid))).statusCode(200);

        assertTrue(messagesWithWamid(wamid).isEmpty());
    }

    @Test
    void metaRemovingTheAccountDisconnectsItsLine() {
        var owner = TestTenants.owner();
        var line = TestWhatsapp.newPhoneNumberId();
        var wabaId = TestWhatsapp.newPhoneNumberId();
        TestWhatsapp.connect(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), line, wabaId)
                .statusCode(201);

        post(payload("partner-removed", Map.of("WABA_ID", wabaId))).statusCode(200);

        assertEquals("disconnected", lineStatus(line));
    }

    @Test
    void templateStatusesReachEveryLineOfTheAccount() {
        var owner = TestTenants.owner();
        var establishmentId = TestTenants.firstEstablishmentOf(owner);
        var wabaId = TestWhatsapp.newPhoneNumberId();
        TestWhatsapp.connect(owner, establishmentId, UUID.randomUUID(), TestWhatsapp.newPhoneNumberId(), wabaId)
                .statusCode(201).body("templates.reminder", equalTo("approved"));

        post(payload("template-status", Map.of("WABA_ID", wabaId, "EVENT", "PAUSED",
                "TEMPLATE_NAME", "takodu_reminder"))).statusCode(200);
        post(payload("template-status", Map.of("WABA_ID", wabaId, "EVENT", "REJECTED",
                "TEMPLATE_NAME", "someone_elses_template"))).statusCode(200);
        post(payload("template-status", Map.of("WABA_ID", wabaId, "EVENT", "FLAGGED",
                "TEMPLATE_NAME", "takodu_confirmation"))).statusCode(200);

        given().spec(owner.spec()).get("/api/v1/establishments/" + establishmentId + "/whatsapp-line")
                .then().statusCode(200)
                .body("templates.reminder", equalTo("paused"))
                .body("templates.confirmation", equalTo("approved"))
                .body("templates.cancellation", equalTo("approved"));
    }

    @Test
    void signedBodyThatIsNotJsonIsRefused() {
        post("not json").statusCode(400);
    }

    @Test
    void otherObjectsAreAcknowledged() {
        post("{\"object\": \"page\", \"entry\": []}").statusCode(200);
    }

    @Test
    void handshakeEchoesTheChallengeOnlyForTheVerifyToken() {
        given().queryParam("hub.mode", "subscribe").queryParam("hub.verify_token", "dev-verify-token")
                .queryParam("hub.challenge", "1158201444")
                .get("/webhooks/whatsapp").then().statusCode(200).body(equalTo("1158201444"));
        given().queryParam("hub.mode", "subscribe").queryParam("hub.verify_token", "guess")
                .queryParam("hub.challenge", "1158201444")
                .get("/webhooks/whatsapp").then().statusCode(401);
    }

    private static String status(String line, String waId, String wamid, String status, String errors) {
        return payload("status", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId, "WAMID", wamid, "STATUS", status,
                "ERRORS", errors));
    }

    private static String connectedLine() {
        var owner = TestTenants.owner();
        return TestWhatsapp.connectLine(owner, TestTenants.firstEstablishmentOf(owner));
    }
}
