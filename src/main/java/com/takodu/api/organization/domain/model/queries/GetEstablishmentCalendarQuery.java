package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * An establishment's time zone and opening hours, for contexts that book in it. Answered without checking the
 * caller, because jobs and the assistant ask it too; the asking context checks access.
 */
public record GetEstablishmentCalendarQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetEstablishmentCalendarQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
