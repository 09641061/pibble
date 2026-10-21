package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Email a new link, valid another 7 days, for an invitation that was lost or expired.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param inviterId the caller's membership, from {@code TenantContext}; named in the email
 */
public record RenewInvitationCommand(OrganizationId organizationId, MemberId inviterId, MemberId memberId,
                                     IdempotencyKey idempotencyKey) {
    public RenewInvitationCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (inviterId == null) throw new IllegalArgumentException("inviterId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
