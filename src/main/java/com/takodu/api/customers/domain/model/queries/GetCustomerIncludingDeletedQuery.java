package com.takodu.api.customers.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * A customer as past appointments and conversations show it, deleted or not (C5).
 *
 * @param organizationId the organization the caller acts in
 */
public record GetCustomerIncludingDeletedQuery(OrganizationId organizationId, CustomerId customerId) {
    public GetCustomerIncludingDeletedQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
    }
}
