package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.infrastructure.persistence.jpa.entities.SessionPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SessionPersistenceRepository implements PanacheRepositoryBase<SessionPersistenceEntity, UUID> {
    public List<SessionPersistenceEntity> findActiveByUser(UUID userId, Instant now) {
        return list("userId = ?1 and revokedAt is null and expiresAt > ?2 order by lastUsedAt desc", userId, now);
    }

    public Optional<SessionPersistenceEntity> findByIdAndUser(UUID id, UUID userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<SessionPersistenceEntity> findByRefreshTokenForUpdate(SecretHash hash) {
        return find("refreshTokenHash = ?1 or previousRefreshTokenHash = ?1", hash)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
}
