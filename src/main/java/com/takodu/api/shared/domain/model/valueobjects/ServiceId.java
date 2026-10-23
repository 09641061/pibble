package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a bookable service of an establishment.
 *
 * @param value the UUID primary key
 */
public record ServiceId(UUID value) {
    public ServiceId {
        if (value == null) throw new IllegalArgumentException("ServiceId is required");
    }
}
