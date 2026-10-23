package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

/**
 * @param staffMemberId the blocked member; null blocks the whole establishment
 * @param requestedBy the caller's membership, from {@code TenantContext}; decides whether the block is their own
 */
public record CreateTimeBlockCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                     MemberId staffMemberId, TimeRange range, String reason, MemberId requestedBy,
                                     IdempotencyKey idempotencyKey) {
    public CreateTimeBlockCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (range == null) throw new IllegalArgumentException("start and end are required");
        if (requestedBy == null) throw new IllegalArgumentException("requestedBy is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
