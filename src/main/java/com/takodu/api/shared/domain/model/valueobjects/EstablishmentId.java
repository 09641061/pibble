package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one location (branch) of an organization.
 *
 * @param value the UUID primary key
 */
public record EstablishmentId(UUID value) {
    public EstablishmentId {
        if (value == null) throw new IllegalArgumentException("EstablishmentId is required");
    }
}
