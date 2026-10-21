package com.takodu.api.customers.domain.model.commands;

import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * C3, C4: a customer typed in by staff.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param document optional
 */
public record RegisterCustomerCommand(OrganizationId organizationId, PhoneNumber phone, String name,
                                      IdentityDocument document, String notes, IdempotencyKey idempotencyKey) {
    public RegisterCustomerCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (phone == null) throw new IllegalArgumentException("Phone is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
