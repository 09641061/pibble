package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.EstablishmentPersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.EstablishmentPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class EstablishmentRepositoryImpl implements EstablishmentRepository {
    private static final String IDEMPOTENCY_UNIQUE = "establishments_idempotency_uk";

    private final EstablishmentPersistenceRepository establishmentPersistenceRepository;

    public EstablishmentRepositoryImpl(EstablishmentPersistenceRepository establishmentPersistenceRepository) {
        this.establishmentPersistenceRepository = establishmentPersistenceRepository;
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
