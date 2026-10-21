package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Duration;
import java.util.Set;

/**
 * O14, O15: the whole editable state of a service; booked appointments keep their price (B8).
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record UpdateServiceCommand(OrganizationId organizationId, ServiceId serviceId, String name, Duration duration,
                                   Money price, String instructions, Set<MemberId> performerIds) {
    public UpdateServiceCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (serviceId == null) throw new IllegalArgumentException("serviceId is required");
        performerIds = performerIds == null ? Set.of() : Set.copyOf(performerIds);
    }
}
