package com.takodu.api.customers.domain.model.commands;

import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * C4: the whole editable state of a customer.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param document optional; {@code null} removes it
 */
public record UpdateCustomerCommand(OrganizationId organizationId, CustomerId customerId, String name,
                                    IdentityDocument document, String notes) {
    public UpdateCustomerCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
    }
}
