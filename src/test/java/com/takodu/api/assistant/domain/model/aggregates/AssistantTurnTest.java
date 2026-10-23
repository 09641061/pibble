package com.takodu.api.assistant.domain.model.aggregates;

import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantTurnTest {
    private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");

    @Test
    void failedAttemptsAreRetriedLaterUntilTheThird() {
        var turn = enqueue("text");

        turn.begin(NOW);
        turn.fail("timeout", null, 0, NOW);
        assertEquals(TurnStatus.PENDING, turn.getStatus());
        assertEquals(NOW.plusSeconds(30), turn.getRunAfter());

        turn.begin(NOW.plusSeconds(30));
        turn.fail("timeout", null, 0, NOW.plusSeconds(30));
        assertEquals(NOW.plusSeconds(90), turn.getRunAfter());

        turn.begin(NOW.plusSeconds(90));
        turn.fail("timeout", new TurnUsage("deepseek-chat", 900, 0, 0), 2, NOW.plusSeconds(90));
        assertTrue(turn.isFailed());
        assertEquals(3, turn.getAttempts());
        assertEquals(900, turn.getUsage().inputTokens());
        assertEquals(NOW.plusSeconds(90), turn.getFinishedAt());
    }

    @Test
    void runningTurnIsTakenOverOnlyAfterItsLease() {
        var turn = enqueue("text");
        turn.begin(NOW);

        assertThrows(IllegalStateException.class, () -> turn.begin(NOW.plus(Duration.ofMinutes(4))));
        assertTrue(turn.isStale(NOW.plus(AssistantTurn.LEASE)));
        turn.begin(NOW.plus(AssistantTurn.LEASE));
        assertEquals(2, turn.getAttempts());
    }

    @Test
    void completedTurnRecordsItsCostAndWhatItAnswered() {
        var turn = enqueue("text");
        turn.begin(NOW);

        turn.complete(new TurnUsage("deepseek-chat", 1200, 800, 90), 3, NOW.plusSeconds(20), NOW.plusSeconds(25));

        assertEquals(TurnStatus.DONE, turn.getStatus());
        assertEquals(3, turn.getToolCalls());
        assertEquals(800, turn.getUsage().cachedTokens());
        assertThrows(IllegalStateException.class, () -> turn.skip("late", NOW));
    }

    @Test
    void replyThroughALaterMessageAnswersThisOne() {
        var turn = enqueue("text");

        assertTrue(turn.isAnsweredBy(NOW));
        assertTrue(turn.isAnsweredBy(NOW.plusSeconds(5)));
        assertFalse(turn.isAnsweredBy(NOW.minusSeconds(1)));
        assertFalse(turn.isAnsweredBy(null));
    }

    @Test
    void assistantReadsTextAndTapsButNotMedia() {
        assertTrue(enqueue("TEXT").triggerIsReadable());
        assertTrue(enqueue("interactive").triggerIsReadable());
        assertTrue(enqueue("button").triggerIsReadable());
        assertFalse(enqueue("audio").triggerIsReadable());
        assertFalse(enqueue("image").triggerIsReadable());
    }

    private static AssistantTurn enqueue(String kind) {
        return AssistantTurn.enqueue(new OrganizationId(UUID.randomUUID()), new EstablishmentId(UUID.randomUUID()),
                new ConversationId(UUID.randomUUID()), new CustomerId(UUID.randomUUID()),
                new MessageId(UUID.randomUUID()), kind, NOW, NOW);
    }
}
