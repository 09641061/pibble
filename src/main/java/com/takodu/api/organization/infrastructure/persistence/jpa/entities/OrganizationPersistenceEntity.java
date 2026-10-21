package com.takodu.api.organization.infrastructure.persistence.jpa.entities;

import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.organization.infrastructure.persistence.jpa.converters.PlanPersistenceConverter;
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
@Table(name = "organizations")
@Getter
@Setter
public class OrganizationPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Convert(converter = PlanPersistenceConverter.class)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "plan_active_until")
    private Instant planActiveUntil;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;

    @Column(name = "last_idempotency_key")
    private UUID lastIdempotencyKey;
}
