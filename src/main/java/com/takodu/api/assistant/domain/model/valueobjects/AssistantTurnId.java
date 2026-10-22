package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record AssistantTurnId(UUID value) {
    public AssistantTurnId {
        if (value == null) throw new IllegalArgumentException("AssistantTurnId is required");
    }
}
