package com.takodu.api.assistant.domain.model.queries;

import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** A9: the Takodu team reads what each of an organization's assistants used, from the internal API. */
public record GetOrganizationUsageQuery(OrganizationId organizationId, UsagePeriod period) {
    public GetOrganizationUsageQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (period == null) throw new IllegalArgumentException("period is required");
    }
}
