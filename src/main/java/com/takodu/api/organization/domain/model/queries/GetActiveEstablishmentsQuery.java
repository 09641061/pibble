package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record GetActiveEstablishmentsQuery(OrganizationId organizationId) {
    public GetActiveEstablishmentsQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
    }
}
