package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.List;
import java.util.Optional;

/** Append-only: there is no update or delete. */
public interface PlanChangeRepository {
    /** @return the organization's plan changes, newest first */
    List<PlanChange> findByOrganization(OrganizationId organizationId);

    Optional<PlanChange> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    PlanChange add(PlanChange planChange);
}
