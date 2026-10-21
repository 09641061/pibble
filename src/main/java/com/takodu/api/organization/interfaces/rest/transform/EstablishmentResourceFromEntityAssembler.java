package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.interfaces.rest.resources.EstablishmentResource;

public final class EstablishmentResourceFromEntityAssembler {
    private EstablishmentResourceFromEntityAssembler() { }

    public static EstablishmentResource toResourceFromEntity(Establishment establishment) {
        return new EstablishmentResource(establishment.getId().value(), establishment.getName(),
                establishment.getAddress(), establishment.getTimeZone().value().getId(), establishment.getArchivedAt());
    }
}
