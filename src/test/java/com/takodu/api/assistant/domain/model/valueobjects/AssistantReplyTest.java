package com.takodu.api.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantReplyTest {

    @Test
    void optionsBecomeNumberedLinesUnderTheText() {
        var reply = new AssistantReply(" ¿A qué hora? ", Arrays.asList("10:00", " ", null, "10:30 "));

        assertEquals("¿A qué hora?\n1. 10:00\n2. 10:30", reply.asText());
    }

    @Test
    void replyIsCutToAThousandCharacters() {
        var reply = new AssistantReply("a".repeat(1200), List.of());

        assertEquals(1000, reply.asText().length());
        assertEquals('…', reply.asText().charAt(999));
    }

    @Test
    void replyOffersAtMostTenOptionsAndIsNeverEmpty() {
        var many = new AssistantReply("", List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));

        assertEquals(10, many.options().size());
        assertThrows(IllegalArgumentException.class, () -> new AssistantReply(" ", List.of()));
    }
}
