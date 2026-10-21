package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a tenant: the business that pays for Takodu.
 *
 * @param value the UUID primary key
 */
public record OrganizationId(UUID value) {
    public OrganizationId {
        if (value == null) throw new IllegalArgumentException("OrganizationId is required");
    }
}
