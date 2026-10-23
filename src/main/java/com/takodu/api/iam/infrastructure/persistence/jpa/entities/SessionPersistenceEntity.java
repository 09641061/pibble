package com.takodu.api.iam.infrastructure.persistence.jpa.entities;

import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.infrastructure.persistence.jpa.converters.SecretHashPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
public class SessionPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Convert(converter = SecretHashPersistenceConverter.class)
    @Column(name = "refresh_token_hash", nullable = false)
    private SecretHash refreshTokenHash;

    @Convert(converter = SecretHashPersistenceConverter.class)
    @Column(name = "previous_refresh_token_hash")
    private SecretHash previousRefreshTokenHash;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;
}
