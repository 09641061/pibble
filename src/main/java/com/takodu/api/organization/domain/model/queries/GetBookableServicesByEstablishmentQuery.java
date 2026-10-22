package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** The establishment's active services with their performers, by name. Answered without checking the caller. */
public record GetBookableServicesByEstablishmentQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetBookableServicesByEstablishmentQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
