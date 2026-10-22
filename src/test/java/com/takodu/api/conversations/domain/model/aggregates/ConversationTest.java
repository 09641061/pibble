package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {
    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Test
    void windowIsOpenUntilExactly24HoursAfterTheCustomerWrote() {
        var conversation = start();
        conversation.recordInbound(NOW);

        assertTrue(conversation.isWindowOpen(NOW.plus(Duration.ofHours(24)).minusMillis(1)));
        assertFalse(conversation.isWindowOpen(NOW.plus(Duration.ofHours(24))));
    }

    @Test
    void windowIsClosedWhenTheCustomerNeverWrote() {
        var conversation = start();
        conversation.recordHistory(NOW);
        conversation.recordHumanOutbound(NOW);

        assertFalse(conversation.isWindowOpen(NOW));
    }

    @Test
    void lateDeliveryOfAnOlderMessageDoesNotShortenTheWindow() {
        var conversation = start();
        conversation.recordInbound(NOW);

        conversation.recordInbound(NOW.minus(Duration.ofHours(5)));

        assertEquals(NOW, conversation.getLastInboundAt());
        assertEquals(2, conversation.getUnreadCount());
    }

    @Test
    void humanReplyPausesTheAssistant12HoursAndClearsUnread() {
        var conversation = start();
        conversation.recordInbound(NOW);

        conversation.recordHumanOutbound(NOW.plusSeconds(60));

        assertEquals(0, conversation.getUnreadCount());
        assertTrue(conversation.isAssistantPaused(NOW.plus(Duration.ofHours(12))));
        assertFalse(conversation.isAssistantPaused(NOW.plusSeconds(60).plus(Duration.ofHours(12))));
    }

    @Test
    void olderEchoDoesNotShortenThePause() {
        var conversation = start();
        conversation.recordHumanOutbound(NOW);

        conversation.recordHumanOutbound(NOW.minus(Duration.ofHours(3)));

        assertEquals(NOW.plus(Duration.ofHours(12)), conversation.getAssistantPausedUntil());
    }

    @Test
    void inboxOrderFollowsTheNewestMessage() {
        var conversation = start();

        conversation.recordHistory(NOW.minus(Duration.ofDays(3)));
        conversation.recordInbound(NOW.plusSeconds(5));

        assertEquals(NOW.plusSeconds(5), conversation.getLastMessageAt());
    }

    private static Conversation start() {
        var line = new WhatsappLine(new WhatsappLineId(UUID.randomUUID()), new OrganizationId(UUID.randomUUID()),
                new EstablishmentId(UUID.randomUUID()), "1", "2", "+51", new EncryptedToken("t"), LineMode.COEXISTENCE,
                true, NOW, null, Map.of());
        return Conversation.start(line, new CustomerId(UUID.randomUUID()), NOW.minus(Duration.ofDays(30)));
    }
}
