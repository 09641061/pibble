package com.takodu.api.conversations.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.takodu.api.conversations.domain.model.valueobjects.InteractiveChoices.Style.BUTTONS;
import static com.takodu.api.conversations.domain.model.valueobjects.InteractiveChoices.Style.LIST;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InteractiveChoicesTest {

    @Test
    void buttonsAreAtMostThreeDifferentTitlesOfTwentyCharacters() {
        new InteractiveChoices(BUTTONS, List.of("a".repeat(20), "b", "c"));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(BUTTONS, List.of("a", "b", "c", "d")));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(BUTTONS, List.of("a".repeat(21))));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(BUTTONS, List.of("a", "a")));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(BUTTONS, List.of()));
    }

    @Test
    void listIsAtMostTenRowsOfTwentyFourCharacters() {
        new InteractiveChoices(LIST, Collections.nCopies(10, "a".repeat(24)));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(LIST, Collections.nCopies(11, "a")));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(LIST, List.of("a".repeat(25))));
        assertThrows(IllegalArgumentException.class, () -> new InteractiveChoices(LIST, List.of(" ")));
    }
}
