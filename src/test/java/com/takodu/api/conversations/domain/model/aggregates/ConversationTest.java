package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void handOffKeepsTheAssistantQuietUntilResumedAndLeavesTheChatUnread() {
        var conversation = start();
        conversation.recordInbound(NOW);
        conversation.markRead();

        conversation.handOff("  Quiere hablar con la dueña  ", NOW);

        assertTrue(conversation.isAssistantPaused(NOW.plus(Duration.ofDays(30))));
        assertEquals("Quiere hablar con la dueña", conversation.getHandoffSummary());
        assertEquals(1, conversation.getUnreadCount());
        assertThrows(IllegalArgumentException.class, () -> conversation.handOff(" ", NOW));
    }

    @Test
    void assistantReplyLeavesTheCustomersMessagesUnread() {
        var conversation = start();
        conversation.recordInbound(NOW);

        conversation.recordAssistantOutbound(NOW.plusSeconds(5));

        assertEquals(1, conversation.getUnreadCount());
        assertFalse(conversation.isAssistantPaused(NOW.plusSeconds(5)));
        assertEquals(NOW.plusSeconds(5), conversation.getLastMessageAt());
    }

    @Test
    void manualPauseLastsUntilResumedAndRecordsWhoPausedFirst() {
        var conversation = start();
        conversation.recordInbound(NOW);
        var owner = new MemberId(UUID.randomUUID());

        conversation.pauseAssistant(owner, NOW, new IdempotencyKey(UUID.randomUUID()));
        conversation.pauseAssistant(new MemberId(UUID.randomUUID()), NOW.plusSeconds(5),
                new IdempotencyKey(UUID.randomUUID()));

        assertTrue(conversation.isAssistantPaused(NOW.plus(Duration.ofDays(30))));
        assertEquals(owner, conversation.getManuallyPausedBy());
        assertEquals(NOW, conversation.getManuallyPausedAt());
        assertEquals(1, conversation.getUnreadCount());
        assertFalse(conversation.isHandedOff());
    }

    @Test
    void resumeClearsTheHumanPauseTheManualPauseAndAHandoff() {
        var conversation = start();
        conversation.recordInbound(NOW);
        conversation.recordHumanOutbound(NOW);
        conversation.pauseAssistant(new MemberId(UUID.randomUUID()), NOW, new IdempotencyKey(UUID.randomUUID()));
        conversation.handOff("Pide hablar con una persona", NOW);

        conversation.resumeAssistant(new IdempotencyKey(UUID.randomUUID()));

        assertFalse(conversation.isAssistantPaused(NOW));
        assertFalse(conversation.isManuallyPaused());
        assertFalse(conversation.isHandedOff());
        assertNull(conversation.getHandoffSummary());
        assertNull(conversation.getAssistantPausedUntil());
    }

    @Test
    void aKeyIsAppliedOnce() {
        var conversation = start();
        var key = new IdempotencyKey(UUID.randomUUID());

        conversation.resumeAssistant(key);

        assertTrue(conversation.alreadyApplied(key));
        assertFalse(conversation.alreadyApplied(new IdempotencyKey(UUID.randomUUID())));
    }

    private static Conversation start() {
        var line = new WhatsappLine(new WhatsappLineId(UUID.randomUUID()), new OrganizationId(UUID.randomUUID()),
                new EstablishmentId(UUID.randomUUID()), "1", "2", "+51", new EncryptedToken("t"), LineMode.COEXISTENCE,
                true, NOW, null, Map.of());
        return Conversation.start(line, new CustomerId(UUID.randomUUID()), NOW.minus(Duration.ofDays(30)));
    }
}
