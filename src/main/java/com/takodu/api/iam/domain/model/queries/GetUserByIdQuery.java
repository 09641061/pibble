package com.takodu.api.iam.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * @param userId the user to look up
 */
public record GetUserByIdQuery(UserId userId) {
    public GetUserByIdQuery {
        if (userId == null) throw new IllegalArgumentException("userId is required");
    }
}
