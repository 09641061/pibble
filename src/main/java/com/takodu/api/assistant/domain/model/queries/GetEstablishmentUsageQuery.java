package com.takodu.api.assistant.domain.model.queries;

import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** A9: an owner or manager reads what the establishment's assistant used. */
public record GetEstablishmentUsageQuery(OrganizationId organizationId, EstablishmentId establishmentId,
                                         UsagePeriod period) {
    public GetEstablishmentUsageQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (period == null) throw new IllegalArgumentException("period is required");
    }
}
