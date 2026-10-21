package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record MessageId(UUID value) {
    public MessageId {
        if (value == null) throw new IllegalArgumentException("MessageId is required");
    }
}
