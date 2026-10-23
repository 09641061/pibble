package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Changes an establishment's name, address and time zone.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record DescribeEstablishmentCommand(OrganizationId organizationId, EstablishmentId establishmentId, String name,
                                           String address, IanaZone timeZone) {
    public DescribeEstablishmentCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (timeZone == null) throw new IllegalArgumentException("timeZone is required");
    }
}
