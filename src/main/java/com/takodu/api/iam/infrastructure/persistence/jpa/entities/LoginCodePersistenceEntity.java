package com.takodu.api.iam.infrastructure.persistence.jpa.entities;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.infrastructure.persistence.jpa.converters.EmailPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.converters.SecretHashPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "login_codes")
@Getter
@Setter
public class LoginCodePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Convert(converter = EmailPersistenceConverter.class)
    @Column(name = "email", nullable = false, updatable = false)
    private Email email;

    @Convert(converter = SecretHashPersistenceConverter.class)
    @Column(name = "code_hash", nullable = false, updatable = false)
    private SecretHash codeHash;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}
