package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.OrganizationPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrganizationPersistenceRepository implements PanacheRepositoryBase<OrganizationPersistenceEntity, UUID> {
    public Optional<OrganizationPersistenceEntity> findByIdForUpdate(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Optional<OrganizationPersistenceEntity> findByIdempotencyKey(UUID key) {
        return find("idempotencyKey", key).firstResultOptional();
    }
}
