package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.domain.model.queries.GetBookableServiceQuery;
import com.takodu.api.organization.domain.model.queries.GetBookableServicesByEstablishmentQuery;
import com.takodu.api.organization.domain.model.queries.GetServiceByIdQuery;
import com.takodu.api.organization.domain.model.queries.GetServicesByEstablishmentQuery;

import java.util.List;
import java.util.Optional;

public interface ServiceQueryService {
    /**
     * @return active and inactive services, by name
     * @throws com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException when the establishment is
     *         not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is staff not assigned to
     *         it (O13)
     */
    List<Service> handle(GetServicesByEstablishmentQuery query);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is staff not assigned to
     *         the service's establishment (O13)
     */
    Optional<Service> handle(GetServiceByIdQuery query);

    /** @return the service, active or not, with its eligible performers; empty when not in the organization */
    Optional<BookableService> handle(GetBookableServiceQuery query);

    List<BookableService> handle(GetBookableServicesByEstablishmentQuery query);
}
