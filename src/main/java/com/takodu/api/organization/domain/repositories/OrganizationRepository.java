package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    Optional<Organization> findById(OrganizationId organizationId);

    @CrossTenant("the Takodu team's internal API lists every organization")
    List<Organization> findAll();

    /** Locks the row until commit, so checks that count the organization's establishments or owners serialize. */
    Optional<Organization> findByIdForUpdate(OrganizationId organizationId);

    @CrossTenant("organization creation: the organization does not exist yet; the service checks the caller founded it")
    Optional<Organization> findByIdempotencyKey(IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Organization save(Organization organization);
}
