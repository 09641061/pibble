package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** The organization's plan history (O5), newest first. */
public record GetPlanChangesQuery(OrganizationId organizationId) {
    public GetPlanChangesQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
    }
}
