package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record ConversationId(UUID value) {
    public ConversationId {
        if (value == null) throw new IllegalArgumentException("ConversationId is required");
    }
}
