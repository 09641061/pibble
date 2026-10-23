package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.ServicePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ServicePersistenceRepository implements PanacheRepositoryBase<ServicePersistenceEntity, UUID> {
    public Optional<ServicePersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public List<ServicePersistenceEntity> findByEstablishment(UUID organizationId, UUID establishmentId) {
        return list("organizationId = ?1 and establishmentId = ?2 order by name", organizationId, establishmentId);
    }

    public List<ServicePersistenceEntity> findByPerformer(UUID organizationId, UUID memberId) {
        return list("organizationId = ?1 and ?2 member of performerIds", organizationId, memberId);
    }

    public Optional<ServicePersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
}
