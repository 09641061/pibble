package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record ScheduledMessageId(UUID value) {
    public ScheduledMessageId {
        if (value == null) throw new IllegalArgumentException("ScheduledMessageId is required");
    }
}
