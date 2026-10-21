package com.takodu.api.customers.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record GetCustomerByIdQuery(OrganizationId organizationId, CustomerId customerId) {
    public GetCustomerByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
    }
}
