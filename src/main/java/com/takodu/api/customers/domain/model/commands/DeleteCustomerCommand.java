package com.takodu.api.customers.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * C5: a soft delete.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record DeleteCustomerCommand(OrganizationId organizationId, CustomerId customerId) {
    public DeleteCustomerCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
    }
}
