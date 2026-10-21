package com.takodu.api.shared.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a user's membership in one organization.
 *
 * @param value the UUID primary key
 */
public record MemberId(UUID value) {
    public MemberId {
        if (value == null) throw new IllegalArgumentException("MemberId is required");
    }
}
