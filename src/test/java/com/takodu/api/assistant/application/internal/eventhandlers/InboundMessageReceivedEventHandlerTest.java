package com.takodu.api.assistant.application.internal.eventhandlers;

import com.takodu.api.assistant.TestAssistant;
import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.assistant.AssistantRows.turnsOf;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.reply;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** A2: a customer message stored by the real webhook queues a turn only when the assistant may answer. */
@QuarkusTest
class InboundMessageReceivedEventHandlerTest {

    @Inject
    TestClock clock;

    @BeforeEach
    void eightInTheMorningInLima() {
        clock.set(TestSalon.at(clock, 1, "08:00"));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void customerMessageQueuesOneTurn() {
        var salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "Aceptamos Yape.");
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());

        var conversationId = customerWrites(salon.owner(), salon.establishmentId(), line, MetaWebhooks.newWaId(),
                clock.instant());

        var turns = turnsOf(conversationId);
        assertEquals(1, turns.size());
        assertEquals("pending", turns.getFirst().get("status"));
        assertEquals("text", turns.getFirst().get("trigger_kind"));
        assertEquals(salon.establishmentId(), turns.getFirst().get("establishment_id"));
    }

    @Test
    void assistantThatIsOffQueuesNothing() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var first = customerWrites(salon.owner(), salon.establishmentId(), line, MetaWebhooks.newWaId(),
                clock.instant());
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "");
        TestAssistant.disable(salon.owner(), salon.establishmentId());

        var second = customerWrites(salon.owner(), salon.establishmentId(), line, MetaWebhooks.newWaId(),
                clock.instant());

        assertEquals(List.of(), turnsOf(first));
        assertEquals(List.of(), turnsOf(second));
    }

    @Test
    void chatAPersonIsHandlingQueuesNothing() {
        var salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "");
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = MetaWebhooks.newWaId();
        var conversationId = customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        clock.advance(Duration.ofMinutes(1));
        reply(salon.owner(), conversationId, UUID.randomUUID(), "Te atiendo yo").statusCode(201);

        clock.advance(Duration.ofHours(11));
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        assertEquals(1, turnsOf(conversationId).size());

        clock.advance(Duration.ofHours(1));
        customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        assertEquals(2, turnsOf(conversationId).size());
    }

    @Test
    void reactionIsNeverAnswered() {
        var salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "");
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = MetaWebhooks.newWaId();
        var conversationId = customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());

        MetaWebhooks.post(MetaWebhooks.payload("reaction", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId,
                "WAMID", MetaWebhooks.newWamid()))).statusCode(200);

        assertEquals(1, turnsOf(conversationId).size());
    }

    @Test
    void inactivePlanQueuesNothing() {
        var salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "");
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        clock.advance(Duration.ofDays(15));

        var conversationId = customerWrites(salon.owner(), salon.establishmentId(), line, MetaWebhooks.newWaId(),
                clock.instant());

        assertEquals(List.of(), turnsOf(conversationId));
    }
}
