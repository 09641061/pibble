package com.takodu.api.iam.infrastructure.persistence.jpa.entities;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.infrastructure.persistence.jpa.converters.EmailPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Convert(converter = EmailPersistenceConverter.class)
    @Column(name = "email", nullable = false)
    private Email email;

    @Column(name = "name")
    private String name;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
