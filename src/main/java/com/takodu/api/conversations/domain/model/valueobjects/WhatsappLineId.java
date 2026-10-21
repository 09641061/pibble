package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record WhatsappLineId(UUID value) {
    public WhatsappLineId {
        if (value == null) throw new IllegalArgumentException("WhatsappLineId is required");
    }
}
