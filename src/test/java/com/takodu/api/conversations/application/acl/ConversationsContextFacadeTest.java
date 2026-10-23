package com.takodu.api.conversations.application.acl;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.jobs.OutboundMessageSender;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.conversations.interfaces.acl.ChatLineView;
import com.takodu.api.conversations.interfaces.acl.ConversationsContextFacade;
import com.takodu.api.conversations.interfaces.acl.ReplyChoicesView;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.takodu.api.conversations.TestInbox.conversation;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static com.takodu.api.conversations.TestInbox.reply;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The assistant reads a chat, answers through the outbox and hands it off, through the facade. */
@QuarkusTest
class ConversationsContextFacadeTest {
    @Inject
    TestClock clock;

    @Inject
    ConversationsContextFacade conversations;

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
    void customerMessageOpensTheChatToTheAssistant() {
        var salon = TestSalon.open();
        var conversationId = customerWritesNow(salon, newWaId());

        var view = conversations.fetchConversation(salon.owner().organizationId(), conversationId).orElseThrow();

        assertEquals(salon.establishmentId(), view.establishmentId());
        assertTrue(view.lineConnected());
        assertTrue(view.windowOpen());
        assertFalse(view.assistantPaused());
        assertTrue(conversations.fetchConversation(TestTenants.owner().organizationId(), conversationId).isEmpty());
    }

    @Test
    void replyIsQueuedOnceAndSent() {
        var salon = TestSalon.open();
        var waId = newWaId();
        var conversationId = customerWritesNow(salon, waId);
        var key = UUID.randomUUID();
        clock.advance(Duration.ofMinutes(1));

        assertTrue(conversations.sendAssistantReply(salon.owner().organizationId(), conversationId, "¡Hola!", null, key));
        assertTrue(conversations.sendAssistantReply(salon.owner().organizationId(), conversationId, "¡Hola!", null, key));
        sender.sendDue();

        assertEquals(List.of("¡Hola!"), gateway.sentTo(waId));
        messages(salon.owner(), conversationId).statusCode(200)
                .body("items[0].origin", equalTo("assistant"))
                .body("items[0].status", equalTo("sent"));
        conversation(salon.owner(), conversationId).body("unreadCount", equalTo(1));
    }

    @Test
    void replyWithButtonsGoesAsButtonsAndShowsItsChoices() {
        var salon = TestSalon.open();
        var waId = newWaId();
        var conversationId = customerWritesNow(salon, waId);
        clock.advance(Duration.ofMinutes(1));

        assertTrue(conversations.sendAssistantReply(salon.owner().organizationId(), conversationId, "¿A qué hora?",
                new ReplyChoicesView(ReplyChoicesView.Style.BUTTONS, List.of("10:00", "11:00")), UUID.randomUUID()));
        sender.sendDue();

        assertEquals(List.of("buttons(¿A qué hora?|10:00|11:00)"), gateway.sentTo(waId));
        messages(salon.owner(), conversationId).statusCode(200)
                .body("items[0].kind", equalTo("interactive"))
                .body("items[0].body", equalTo("¿A qué hora?"))
                .body("items[0].choicesStyle", equalTo("buttons"))
                .body("items[0].choices", equalTo(List.of("10:00", "11:00")))
                .body("items[1].choices", equalTo(List.of()));
        assertEquals("¿A qué hora?\n1. 10:00\n2. 11:00", conversations.fetchRecentMessages(
                salon.owner().organizationId(), conversationId, 20).getLast().text());
    }

    @Test
    void replyWithAListGoesAsAListBehindItsButton() {
        var salon = TestSalon.open();
        var waId = newWaId();
        var conversationId = customerWritesNow(salon, waId);
        clock.advance(Duration.ofMinutes(1));

        assertTrue(conversations.sendAssistantReply(salon.owner().organizationId(), conversationId, "Elige uno",
                new ReplyChoicesView(ReplyChoicesView.Style.LIST, List.of("Corte", "Tinte", "Manicure", "Pedicure")),
                UUID.randomUUID()));
        sender.sendDue();

        assertEquals(List.of("list(Elige uno|Ver opciones|Corte|Tinte|Manicure|Pedicure)"), gateway.sentTo(waId));
        messages(salon.owner(), conversationId).statusCode(200).body("items[0].choicesStyle", equalTo("list"));
    }

    @Test
    void replyIsDroppedOnceAPersonAnswered() {
        var salon = TestSalon.open();
        var conversationId = customerWritesNow(salon, newWaId());
        reply(salon.owner(), conversationId, UUID.randomUUID(), "Te atiendo yo").statusCode(201);

        assertTrue(conversations.fetchConversation(salon.owner().organizationId(), conversationId).orElseThrow()
                .assistantPaused());
        assertFalse(conversations.sendAssistantReply(salon.owner().organizationId(), conversationId, "¡Hola!",
                null, UUID.randomUUID()));
    }

    @Test
    void recentMessagesComeOldestFirstWithWhoWroteThem() {
        var salon = TestSalon.open();
        var conversationId = customerWritesNow(salon, newWaId());
        clock.advance(Duration.ofMinutes(1));
        reply(salon.owner(), conversationId, UUID.randomUUID(), "Te atiendo yo").statusCode(201);

        var lines = conversations.fetchRecentMessages(salon.owner().organizationId(), conversationId, 20);

        assertEquals(List.of("customer", "staff"), lines.stream().map(ChatLineView::role).toList());
        assertEquals("Te atiendo yo", lines.getLast().text());
    }

    @Test
    void handOffPausesTheAssistantAndShowsWhy() {
        var salon = TestSalon.open();
        var conversationId = customerWritesNow(salon, newWaId());
        markRead(salon, conversationId);

        conversations.handOff(salon.owner().organizationId(), conversationId, "Quiere un reembolso");

        conversation(salon.owner(), conversationId).statusCode(200)
                .body("assistantHandedOff", equalTo(true))
                .body("assistantPaused", equalTo(true))
                .body("handoffSummary", equalTo("Quiere un reembolso"))
                .body("unreadCount", equalTo(1));
    }

    /** Staff read the chat first, so the handoff is what leaves it unread. */
    private static void markRead(TestSalon salon, UUID conversationId) {
        given().spec(salon.owner().spec()).post("/api/v1/conversations/" + conversationId + "/read")
                .then().statusCode(200);
        conversation(salon.owner(), conversationId).body("unreadCount", equalTo(0));
    }

    private static String newWaId() {
        return "519" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private UUID customerWritesNow(TestSalon salon, String waId) {
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        return customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
    }
}
