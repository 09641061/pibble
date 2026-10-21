package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.infrastructure.persistence.jpa.entities.SessionPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SessionPersistenceRepository implements PanacheRepositoryBase<SessionPersistenceEntity, UUID> {
    public Optional<SessionPersistenceEntity> findByRefreshTokenForUpdate(SecretHash hash) {
        return find("refreshTokenHash = ?1 or previousRefreshTokenHash = ?1", hash)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
}
