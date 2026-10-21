package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * The organizations where the user is an active member, so the app can choose which {@code X-Organization-Id} to
 * send.
 *
 * @param userId the caller, from the access token
 */
public record GetJoinedOrganizationsQuery(UserId userId) {
    public GetJoinedOrganizationsQuery {
        if (userId == null) throw new IllegalArgumentException("userId is required");
    }
}
