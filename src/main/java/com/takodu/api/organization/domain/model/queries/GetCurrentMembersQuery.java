package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * The team: active members and pending invitations.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record GetCurrentMembersQuery(OrganizationId organizationId) {
    public GetCurrentMembersQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
    }
}
