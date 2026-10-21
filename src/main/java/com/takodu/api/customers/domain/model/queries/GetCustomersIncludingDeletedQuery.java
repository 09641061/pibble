package com.takodu.api.customers.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Set;

/**
 * Several customers at once, deleted or not, such as the senders of an inbox page (C5).
 *
 * @param customerIds at most {@link #MAX_IDS}
 */
public record GetCustomersIncludingDeletedQuery(OrganizationId organizationId, Set<CustomerId> customerIds) {
    public static final int MAX_IDS = 200;

    public GetCustomersIncludingDeletedQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerIds == null) throw new IllegalArgumentException("customerIds is required");
        if (customerIds.size() > MAX_IDS) throw new IllegalArgumentException("Ask for at most 200 customers");
        customerIds = Set.copyOf(customerIds);
    }
}
