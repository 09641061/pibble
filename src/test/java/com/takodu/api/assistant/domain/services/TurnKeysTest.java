package com.takodu.api.assistant.domain.services;

import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TurnKeysTest {

    @Test
    void sameTurnActionAndArgumentsGiveTheSameKey() {
        var turn = new AssistantTurnId(UUID.randomUUID());

        assertEquals(TurnKeys.of(turn, "bookAppointment", "corte", "10:00"),
                TurnKeys.of(turn, "bookAppointment", "corte", "10:00"));
        assertNotEquals(TurnKeys.of(turn, "bookAppointment", "corte", "10:00"),
                TurnKeys.of(turn, "bookAppointment", "corte", "10:15"));
        assertNotEquals(TurnKeys.of(turn, "reply"), TurnKeys.of(new AssistantTurnId(UUID.randomUUID()), "reply"));
    }
}
