package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.MemberPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

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

    public Optional<MemberPersistenceEntity> findCurrentById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2 and removedAt is null", organizationId, id).firstResultOptional();
    }

    public List<MemberPersistenceEntity> findCurrent(UUID organizationId) {
        return list("organizationId = ?1 and removedAt is null order by displayName", organizationId);
    }

    public boolean existsCurrentByEmail(UUID organizationId, Email email) {
        return count("organizationId = ?1 and email = ?2 and removedAt is null", organizationId, email) > 0;
    }

    public Optional<MemberPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }

    public Optional<MemberPersistenceEntity> findByInviteTokenForUpdate(SecretHash tokenHash) {
        return find("inviteTokenHash", tokenHash).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}
