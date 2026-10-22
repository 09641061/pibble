package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.UUID;

/** @param value conversations' id of the customer message that triggered the turn */
public record MessageId(UUID value) {
    public MessageId {
        if (value == null) throw new IllegalArgumentException("MessageId is required");
    }
}
