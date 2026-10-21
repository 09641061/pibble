package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

/**
 * A service with the members who can perform it now, for contexts that book it. Answered without checking the
 * caller; the asking context checks access.
 */
public record GetBookableServiceQuery(OrganizationId organizationId, ServiceId serviceId) {
    public GetBookableServiceQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (serviceId == null) throw new IllegalArgumentException("serviceId is required");
    }
}
