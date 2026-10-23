package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.time.Instant;

/**
 * O5: the Takodu team records a payment. The end of the plan is either {@code activeUntil} or the current end
 * extended by {@code extendDays}; with neither, it stays as it is and only the plan changes.
 *
 * @param operatorId the platform operator, from {@code OperatorContext}
 * @param plan the new plan, or null to keep it
 * @param activeUntil the new end of the paid period, or null
 * @param extendDays days to add (O5), or null
 * @param note how and when the business paid
 */
public record ChangePlanCommand(OrganizationId organizationId, UserId operatorId, Plan plan, Instant activeUntil,
                                Integer extendDays, String note, IdempotencyKey idempotencyKey) {
    public ChangePlanCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (operatorId == null) throw new IllegalArgumentException("operatorId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
        if (activeUntil != null && extendDays != null)
            throw new IllegalArgumentException("Send either activeUntil or extendDays, not both");
        if (plan == null && activeUntil == null && extendDays == null)
            throw new IllegalArgumentException("Send a plan, activeUntil or extendDays");
    }
}
