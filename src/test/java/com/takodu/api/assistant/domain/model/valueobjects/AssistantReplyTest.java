package com.takodu.api.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantReplyTest {

    @Test
    void noOptionsIsText() {
        var reply = new AssistantReply(" ¡Listo! ", Arrays.asList(" ", null));

        assertEquals(ReplyFormat.TEXT, reply.format());
        assertEquals("¡Listo!", reply.body());
    }

    @Test
    void threeOptionsOfTwentyCharactersAreButtons() {
        var reply = new AssistantReply("¿Cuál?", List.of("a".repeat(20), "b".repeat(20), "c".repeat(20)));

        assertEquals(ReplyFormat.BUTTONS, reply.format());
        assertEquals("¿Cuál?", reply.body());
    }

    @Test
    void fourOptionsOrATwentyOneCharacterOneAreAList() {
        assertEquals(ReplyFormat.LIST, new AssistantReply("¿Cuál?", List.of("1", "2", "3", "4")).format());
        assertEquals(ReplyFormat.LIST, new AssistantReply("¿Cuál?", List.of("a".repeat(21))).format());
    }

    @Test
    void tenRowsOfTwentyFourCharactersAreAList() {
        var rows = IntStream.range(0, 10).mapToObj(i -> i + "a".repeat(23)).toList();

        assertEquals(ReplyFormat.LIST, new AssistantReply("¿Cuál?", rows).format());
    }

    @Test
    void elevenOptionsOrATwentyFiveCharacterRowAreNumberedText() {
        var eleven = new AssistantReply("¿Cuál?", IntStream.rangeClosed(1, 11).mapToObj(i -> "h" + i).toList());
        var tooLong = new AssistantReply("¿Cuál?", List.of("10:00", "a".repeat(25)));

        assertEquals(ReplyFormat.TEXT, eleven.format());
        assertEquals("¿Cuál?\n1. h1\n2. h2\n3. h3\n4. h4\n5. h5\n6. h6\n7. h7\n8. h8\n9. h9\n10. h10\n11. h11",
                eleven.body());
        assertEquals(ReplyFormat.TEXT, tooLong.format());
        assertEquals("¿Cuál?\n1. 10:00\n2. " + "a".repeat(25), tooLong.body());
    }

    @Test
    void repeatedOptionsAreOfferedOnce() {
        assertEquals(List.of("10:00", "11:00"), new AssistantReply("", List.of("10:00", " 10:00", "11:00")).options());
    }

    @Test
    void choicesWithoutTextGetAPrompt() {
        assertEquals(AssistantReply.CHOOSE, new AssistantReply("", List.of("Sí", "No")).body());
    }

    @Test
    void replyIsCutToAThousandCharacters() {
        var text = new AssistantReply("a".repeat(1200), List.of());
        var buttons = new AssistantReply("a".repeat(1200), List.of("Sí"));
        var numbered = new AssistantReply("a".repeat(990), Collections.nCopies(1, "b".repeat(30)));

        assertEquals(1000, text.body().length());
        assertEquals('…', text.body().charAt(999));
        assertEquals(1000, buttons.body().length());
        assertEquals(1000, numbered.body().length());
        assertThrows(IllegalArgumentException.class, () -> new AssistantReply(" ", List.of()));
    }
}
