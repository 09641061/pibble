package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository {
    Optional<Service> findById(OrganizationId organizationId, ServiceId serviceId);

    /** @return active and inactive services of the establishment, by name */
    List<Service> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId);

    /** @return the services the member performs */
    List<Service> findByPerformer(OrganizationId organizationId, MemberId memberId);

    Optional<Service> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Service save(Service service);
}
