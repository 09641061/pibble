package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import com.takodu.api.conversations.domain.model.valueobjects.MessageOrigin;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageTest {
    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Test
    void staffReplyWaitsInTheOutboxUntilSent() {
        var message = reply("Hola");

        assertEquals(DeliveryStatus.PENDING, message.getStatus());
        assertEquals(NOW, message.getNextAttemptAt());

        message.markSent(new Wamid("wamid.1"));

        assertEquals(DeliveryStatus.SENT, message.getStatus());
        assertNull(message.getNextAttemptAt());
    }

    @Test
    void replyIsTextOfAtMost4096Characters() {
        assertThrows(IllegalArgumentException.class, () -> reply(" "));
        assertThrows(IllegalArgumentException.class, () -> reply("a".repeat(4097)));
        reply("a".repeat(4096));
    }

    @Test
    void failuresAreRetriedWithBackoffAndTheFifthFails() {
        var message = reply("Hola");

        message.recordSendFailure("timeout", true, NOW);
        assertEquals(NOW.plusSeconds(30), message.getNextAttemptAt());
        message.recordSendFailure("timeout", true, NOW);
        assertEquals(NOW.plusSeconds(60), message.getNextAttemptAt());
        message.recordSendFailure("timeout", true, NOW);
        message.recordSendFailure("timeout", true, NOW);
        assertEquals(NOW.plus(Duration.ofMinutes(4)), message.getNextAttemptAt());
        assertEquals(DeliveryStatus.PENDING, message.getStatus());

        message.recordSendFailure("timeout", true, NOW);

        assertEquals(DeliveryStatus.FAILED, message.getStatus());
        assertEquals(5, message.getAttempts());
        assertNull(message.getNextAttemptAt());
    }

    @Test
    void failureMetaWouldRepeatFailsAtOnce() {
        var message = reply("Hola");

        message.recordSendFailure("131047: Re-engagement message", false, NOW);

        assertEquals(DeliveryStatus.FAILED, message.getStatus());
        assertEquals("131047: Re-engagement message", message.getError());
    }

    @Test
    void notificationKeepsTheTextTheCustomerReadsEitherWay() {
        var parameters = List.of("Ana", "Corte", "Sede Miraflores", "sábado 24 de octubre", "10:00");

        var template = Message.notification(conversation(), NotificationTemplate.REMINDER, parameters, true, NOW);
        var text = Message.notification(conversation(), NotificationTemplate.REMINDER, parameters, false, NOW);

        assertEquals(MessageKind.TEMPLATE, template.getContent().kind());
        assertEquals("takodu_reminder", template.getTemplateName());
        assertEquals(parameters, template.getTemplateParameters());
        assertEquals(MessageOrigin.SYSTEM, template.getOrigin());
        assertEquals(MessageKind.TEXT, text.getContent().kind());
        assertNull(text.getTemplateName());
        assertEquals(template.getContent().body(), text.getContent().body());
        assertEquals(DeliveryStatus.PENDING, text.getStatus());
    }

    private static Message reply(String body) {
        return Message.staffText(conversation(), new MemberId(UUID.randomUUID()), body, NOW,
                new IdempotencyKey(UUID.randomUUID()));
    }

    private static Conversation conversation() {
        return new Conversation(new ConversationId(UUID.randomUUID()), new OrganizationId(UUID.randomUUID()),
                new EstablishmentId(UUID.randomUUID()), new WhatsappLineId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()), null, null, NOW, NOW, 1, null, null);
    }
}
