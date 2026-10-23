package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Duration;
import java.util.Set;

/**
 * O14, O15: a new service of an establishment and the members who perform it.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record CreateServiceCommand(OrganizationId organizationId, EstablishmentId establishmentId, String name,
                                   Duration duration, Money price, String instructions, Set<MemberId> performerIds,
                                   IdempotencyKey idempotencyKey) {
    public CreateServiceCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
        performerIds = performerIds == null ? Set.of() : Set.copyOf(performerIds);
    }
}
