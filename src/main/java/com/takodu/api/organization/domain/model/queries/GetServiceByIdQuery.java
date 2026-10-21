package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record GetServiceByIdQuery(OrganizationId organizationId, ServiceId serviceId) {
    public GetServiceByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (serviceId == null) throw new IllegalArgumentException("serviceId is required");
    }
}
