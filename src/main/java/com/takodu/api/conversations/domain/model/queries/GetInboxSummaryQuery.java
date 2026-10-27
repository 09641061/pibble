package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** How many chats of one establishment need someone, for the navigation's badge. */
public record GetInboxSummaryQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetInboxSummaryQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
