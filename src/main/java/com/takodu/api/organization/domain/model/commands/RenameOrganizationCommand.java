package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record RenameOrganizationCommand(OrganizationId organizationId, String name) {
    public RenameOrganizationCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
    }
}
