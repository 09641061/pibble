package com.takodu.api.assistant.domain.services;

import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Idempotency keys for the assistant's writes: the same turn, action and arguments always give the same key, so a
 * retried turn books, reschedules, cancels or replies once (docs/decisions idempotency keys).
 */
public final class TurnKeys {
    private TurnKeys() { }

    /**
     * @param action such as {@code bookAppointment} or {@code reply}
     * @param arguments what the action was asked to do, in a fixed order
     */
    public static UUID of(AssistantTurnId turnId, String action, Object... arguments) {
        var name = new StringBuilder(turnId.value().toString()).append('|').append(action);
        for (var argument : arguments) name.append('|').append(argument);
        return UUID.nameUUIDFromBytes(name.toString().getBytes(StandardCharsets.UTF_8));
    }
}
