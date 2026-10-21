package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.EstablishmentPersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.EstablishmentPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EstablishmentRepositoryImpl implements EstablishmentRepository {
    private static final String IDEMPOTENCY_UNIQUE = "establishments_idempotency_uk";

    private final EstablishmentPersistenceRepository establishmentPersistenceRepository;

    public EstablishmentRepositoryImpl(EstablishmentPersistenceRepository establishmentPersistenceRepository) {
        this.establishmentPersistenceRepository = establishmentPersistenceRepository;
    }

    @Override
    public Optional<Establishment> findById(OrganizationId organizationId, EstablishmentId establishmentId) {
        return establishmentPersistenceRepository.findById(organizationId.value(), establishmentId.value())
                .map(EstablishmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Establishment> findActive(OrganizationId organizationId) {
        return establishmentPersistenceRepository.findActive(organizationId.value()).stream()
                .map(EstablishmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public int countActive(OrganizationId organizationId) {
        return (int) establishmentPersistenceRepository.countActive(organizationId.value());
    }

    @Override
    public Optional<Establishment> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return establishmentPersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(EstablishmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Establishment save(Establishment establishment) {
        var entity = EstablishmentPersistenceAssembler.toPersistenceFromDomain(establishment);
        try {
            if (entity.getId() == null) {
                establishmentPersistenceRepository.persist(entity);
            } else {
                entity = establishmentPersistenceRepository.getEntityManager().merge(entity);
            }
            establishmentPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return EstablishmentPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
