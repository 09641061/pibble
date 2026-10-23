package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a person who signs in, across organizations.
 *
 * @param value the UUID primary key
 */
public record UserId(UUID value) {
    public UserId {
        if (value == null) throw new IllegalArgumentException("UserId is required");
    }
}
