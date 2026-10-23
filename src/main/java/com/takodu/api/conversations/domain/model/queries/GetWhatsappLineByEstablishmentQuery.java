package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** The establishment's connected line, else the one it had last. */
public record GetWhatsappLineByEstablishmentQuery(OrganizationId organizationId, EstablishmentId establishmentId) {
    public GetWhatsappLineByEstablishmentQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
    }
}
