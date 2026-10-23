package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

/**
 * O1: the organization, its first establishment and the creator as owner, in one transaction.
 *
 * @param creatorId the caller, from the access token
 * @param address optional address of the first establishment
 */
public record CreateOrganizationCommand(UserId creatorId, String name, String establishmentName, String address,
                                        IanaZone timeZone, String ownerDisplayName, IdempotencyKey idempotencyKey) {
    public CreateOrganizationCommand {
        if (creatorId == null) throw new IllegalArgumentException("creatorId is required");
        if (timeZone == null) throw new IllegalArgumentException("timeZone is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
