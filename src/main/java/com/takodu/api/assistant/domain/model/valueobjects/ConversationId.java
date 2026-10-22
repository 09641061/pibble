package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.UUID;

/** @param value conversations' id of the chat the turn answers in */
public record ConversationId(UUID value) {
    public ConversationId {
        if (value == null) throw new IllegalArgumentException("ConversationId is required");
    }
}
