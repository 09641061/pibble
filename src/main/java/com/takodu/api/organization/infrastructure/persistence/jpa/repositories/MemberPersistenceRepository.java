package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.MemberPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MemberPersistenceRepository implements PanacheRepositoryBase<MemberPersistenceEntity, UUID> {
    public Optional<MemberPersistenceEntity> findActiveByUser(UUID organizationId, UUID userId) {
        return find("organizationId = ?1 and userId = ?2 and removedAt is null", organizationId, userId)
                .firstResultOptional();
    }

    public List<MemberPersistenceEntity> findActiveByUser(UUID userId) {
        return list("userId = ?1 and removedAt is null", userId);
    }
}
