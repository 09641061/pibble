package com.takodu.api.organization.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one entry in an organization's plan history.
 *
 * @param value the UUID primary key
 */
public record PlanChangeId(UUID value) {
    public PlanChangeId {
        if (value == null) throw new IllegalArgumentException("PlanChangeId is required");
    }
}
