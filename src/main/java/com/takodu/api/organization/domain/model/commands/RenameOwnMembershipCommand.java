package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * A member changes the name the team sees for them in one organization.
 *
 * @param memberId the caller's membership, from {@code TenantContext}
 */
public record RenameOwnMembershipCommand(OrganizationId organizationId, MemberId memberId, String displayName) {
    public RenameOwnMembershipCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
    }
}
