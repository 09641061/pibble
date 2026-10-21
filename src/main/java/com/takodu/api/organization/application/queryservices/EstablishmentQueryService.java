package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.queries.GetActiveEstablishmentsQuery;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentByIdQuery;

import java.util.List;
import java.util.Optional;

public interface EstablishmentQueryService {
    /** @return the active establishments the caller sees (O13), by name */
    List<Establishment> handle(GetActiveEstablishmentsQuery query);

    /**
     * @return the establishment, archived or not, empty when it is not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is staff not assigned to
     *         it (O13)
     */
    Optional<Establishment> handle(GetEstablishmentByIdQuery query);
}
