package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.LoginCodePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LoginCodePersistenceRepository implements PanacheRepositoryBase<LoginCodePersistenceEntity, UUID> {
    public List<LoginCodePersistenceEntity> findUsableForUpdate(Email email, Instant now, int maxAttempts) {
        return find("email = ?1 and consumedAt is null and expiresAt > ?2 and attempts < ?3", email, now, (short) maxAttempts)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .list();
    }

    public long countExpiringAfter(Email email, Instant instant) {
        return count("email = ?1 and expiresAt >= ?2", email, instant);
    }
}
