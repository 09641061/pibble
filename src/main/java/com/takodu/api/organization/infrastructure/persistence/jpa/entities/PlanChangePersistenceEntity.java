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
@Table(name = "plan_changes")
@Getter
@Setter
public class PlanChangePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;

    @Convert(converter = PlanPersistenceConverter.class)
    @Column(name = "from_plan", nullable = false, updatable = false)
    private Plan fromPlan;

    @Convert(converter = PlanPersistenceConverter.class)
    @Column(name = "to_plan", nullable = false, updatable = false)
    private Plan toPlan;

    @Column(name = "from_active_until", updatable = false)
    private Instant fromActiveUntil;

    @Column(name = "to_active_until", nullable = false, updatable = false)
    private Instant toActiveUntil;

    @Column(name = "note", nullable = false, updatable = false)
    private String note;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}
