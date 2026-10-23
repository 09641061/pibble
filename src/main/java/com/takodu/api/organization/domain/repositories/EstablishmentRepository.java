package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.List;
import java.util.Optional;

public interface EstablishmentRepository {
    /** @return the establishment, archived or not, empty when it is not in the organization */
    Optional<Establishment> findById(OrganizationId organizationId, EstablishmentId establishmentId);

    /** @return the organization's active establishments, by name */
    List<Establishment> findActive(OrganizationId organizationId);

    int countActive(OrganizationId organizationId);

    Optional<Establishment> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Establishment save(Establishment establishment);
}
