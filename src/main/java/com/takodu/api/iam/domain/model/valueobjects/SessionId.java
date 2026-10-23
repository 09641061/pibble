package com.takodu.api.iam.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one signed-in device; access tokens carry it as the {@code sid} claim.
 *
 * @param value the UUID primary key
 */
public record SessionId(UUID value) {
    public SessionId {
        if (value == null) throw new IllegalArgumentException("SessionId is required");
    }
}
