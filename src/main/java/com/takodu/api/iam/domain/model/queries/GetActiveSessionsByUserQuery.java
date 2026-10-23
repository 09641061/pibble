package com.takodu.api.iam.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * The devices where the user is signed in.
 *
 * @param userId the caller, from the access token
 */
public record GetActiveSessionsByUserQuery(UserId userId) {
    public GetActiveSessionsByUserQuery {
        if (userId == null) throw new IllegalArgumentException("userId is required");
    }
}
