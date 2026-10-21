package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.LoginCodePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class LoginCodePersistenceRepository implements PanacheRepositoryBase<LoginCodePersistenceEntity, UUID> {
    public long countExpiringAfter(Email email, Instant instant) {
        return count("email = ?1 and expiresAt >= ?2", email, instant);
    }
}
