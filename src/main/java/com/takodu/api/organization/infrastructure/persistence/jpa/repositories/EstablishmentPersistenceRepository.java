package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.EstablishmentPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EstablishmentPersistenceRepository implements PanacheRepositoryBase<EstablishmentPersistenceEntity, UUID> {
    public Optional<EstablishmentPersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public List<EstablishmentPersistenceEntity> findActive(UUID organizationId) {
        return list("organizationId = ?1 and archivedAt is null order by name", organizationId);
    }

    public long countActive(UUID organizationId) {
        return count("organizationId = ?1 and archivedAt is null", organizationId);
    }

    public Optional<EstablishmentPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
}
