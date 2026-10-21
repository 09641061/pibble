package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Establishment;

public interface EstablishmentRepository {
    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Establishment save(Establishment establishment);
}
