package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param address optional street address
 */
public record OpenEstablishmentCommand(OrganizationId organizationId, String name, String address, IanaZone timeZone,
                                       IdempotencyKey idempotencyKey) {
    public OpenEstablishmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (timeZone == null) throw new IllegalArgumentException("timeZone is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
