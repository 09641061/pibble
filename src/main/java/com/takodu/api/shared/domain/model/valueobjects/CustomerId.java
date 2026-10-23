package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a customer of one organization.
 *
 * @param value the UUID primary key
 */
public record CustomerId(UUID value) {
    public CustomerId {
        if (value == null) throw new IllegalArgumentException("CustomerId is required");
    }
}
