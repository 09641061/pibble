package com.takodu.api.iam.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one emailed sign-in code.
 *
 * @param value the UUID primary key
 */
public record LoginCodeId(UUID value) {
    public LoginCodeId {
        if (value == null) throw new IllegalArgumentException("LoginCodeId is required");
    }
}
