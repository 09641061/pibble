package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Removes an active member or revokes a pending invitation (O10).
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record RemoveMemberCommand(OrganizationId organizationId, MemberId memberId) {
    public RemoveMemberCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
    }
}
