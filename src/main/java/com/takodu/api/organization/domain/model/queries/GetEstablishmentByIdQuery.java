package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record GetEstablishmentByIdQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetEstablishmentByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
