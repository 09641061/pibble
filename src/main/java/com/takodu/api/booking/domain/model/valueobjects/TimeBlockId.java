package com.takodu.api.booking.domain.model.valueobjects;

import java.util.UUID;

/** @param value the UUID primary key */
public record TimeBlockId(UUID value) {
    public TimeBlockId {
        if (value == null) throw new IllegalArgumentException("TimeBlockId is required");
    }
}
