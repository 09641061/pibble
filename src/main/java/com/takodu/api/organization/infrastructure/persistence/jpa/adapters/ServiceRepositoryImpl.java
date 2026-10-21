package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.domain.repositories.ServiceRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.ServicePersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.ServicePersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ServiceRepositoryImpl implements ServiceRepository {
    private static final String IDEMPOTENCY_UNIQUE = "services_idempotency_uk";

    private final ServicePersistenceRepository servicePersistenceRepository;

    public ServiceRepositoryImpl(ServicePersistenceRepository servicePersistenceRepository) {
        this.servicePersistenceRepository = servicePersistenceRepository;
    }

    @Override
    public Optional<Service> findById(OrganizationId organizationId, ServiceId serviceId) {
        return servicePersistenceRepository.findById(organizationId.value(), serviceId.value())
                .map(ServicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Service> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId) {
        return servicePersistenceRepository.findByEstablishment(organizationId.value(), establishmentId.value()).stream()
                .map(ServicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Service> findByPerformer(OrganizationId organizationId, MemberId memberId) {
        return servicePersistenceRepository.findByPerformer(organizationId.value(), memberId.value()).stream()
                .map(ServicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Service> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return servicePersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(ServicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Service save(Service service) {
        var entity = ServicePersistenceAssembler.toPersistenceFromDomain(service);
        try {
            if (entity.getId() == null) {
                servicePersistenceRepository.persist(entity);
            } else {
                entity = servicePersistenceRepository.getEntityManager().merge(entity);
            }
            servicePersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return ServicePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
