package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Set;

/**
 * W7: the changes one member may see, fixed when the stream opens.
 *
 * @param allEstablishments true for owners and managers
 * @param establishmentIds the establishments a staff member is assigned to
 */
public record StreamInboxChangesQuery(OrganizationId organizationId, boolean allEstablishments,
                                      Set<EstablishmentId> establishmentIds) {
    public StreamInboxChangesQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        establishmentIds = establishmentIds == null ? Set.of() : Set.copyOf(establishmentIds);
    }

    public boolean sees(EstablishmentId establishmentId) {
        return allEstablishments || establishmentIds.contains(establishmentId);
    }
}
