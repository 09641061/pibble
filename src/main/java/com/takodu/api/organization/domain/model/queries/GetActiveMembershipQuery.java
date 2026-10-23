package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * The membership that lets the user act in the organization.
 *
 * @param organizationId from the {@code X-Organization-Id} header
 * @param userId the caller, from the access token
 */
public record GetActiveMembershipQuery(OrganizationId organizationId, UserId userId) {
    public GetActiveMembershipQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (userId == null) throw new IllegalArgumentException("userId is required");
    }
}
