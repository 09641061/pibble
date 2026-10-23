package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * O12: the owner hands the organization to another member; the roles swap in one transaction.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param callerId the caller's membership, from {@code TenantContext}; must still be the owner once the row is locked
 * @param newOwnerId an active member of the organization
 */
public record TransferOwnershipCommand(OrganizationId organizationId, MemberId callerId, MemberId newOwnerId,
                                       IdempotencyKey idempotencyKey) {
    public TransferOwnershipCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (callerId == null) throw new IllegalArgumentException("callerId is required");
        if (newOwnerId == null) throw new IllegalArgumentException("newOwnerId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
