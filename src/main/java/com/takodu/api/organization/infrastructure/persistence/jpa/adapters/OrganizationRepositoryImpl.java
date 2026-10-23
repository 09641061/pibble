package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.OrganizationPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrganizationRepositoryImpl implements OrganizationRepository {
    private static final String IDEMPOTENCY_UNIQUE = "organizations_idempotency_uk";

    private final OrganizationPersistenceRepository organizationPersistenceRepository;

    public OrganizationRepositoryImpl(OrganizationPersistenceRepository organizationPersistenceRepository) {
        this.organizationPersistenceRepository = organizationPersistenceRepository;
    }

    @Override
    public Optional<Organization> findById(OrganizationId organizationId) {
        return organizationPersistenceRepository.findByIdOptional(organizationId.value())
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Organization> findAll() {
        return organizationPersistenceRepository.listAll().stream()
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<Organization> findByIdForUpdate(OrganizationId organizationId) {
        return organizationPersistenceRepository.findByIdForUpdate(organizationId.value())
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Organization> findByIdempotencyKey(IdempotencyKey idempotencyKey) {
        return organizationPersistenceRepository.findByIdempotencyKey(idempotencyKey.value())
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Organization save(Organization organization) {
        var entity = OrganizationPersistenceAssembler.toPersistenceFromDomain(organization);
        try {
            if (entity.getId() == null) {
                organizationPersistenceRepository.persist(entity);
            } else {
                entity = organizationPersistenceRepository.getEntityManager().merge(entity);
            }
            organizationPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return OrganizationPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
