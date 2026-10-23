package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.PlanChangePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlanChangePersistenceRepository implements PanacheRepositoryBase<PlanChangePersistenceEntity, UUID> {
    public List<PlanChangePersistenceEntity> findByOrganization(UUID organizationId) {
        return list("organizationId", Sort.descending("changedAt"), organizationId);
    }

    public Optional<PlanChangePersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
}
