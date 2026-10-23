package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.organization.domain.model.valueobjects.PlanChangeId;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.PlanChangePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

public final class PlanChangePersistenceAssembler {
    private PlanChangePersistenceAssembler() { }

    public static PlanChange toDomainFromPersistence(PlanChangePersistenceEntity entity) {
        return new PlanChange(new PlanChangeId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new UserId(entity.getChangedBy()), entity.getFromPlan(), entity.getToPlan(),
                entity.getFromActiveUntil(), entity.getToActiveUntil(), entity.getNote(),
                new IdempotencyKey(entity.getIdempotencyKey()), entity.getChangedAt());
    }

    public static PlanChangePersistenceEntity toPersistenceFromDomain(PlanChange change) {
        var entity = new PlanChangePersistenceEntity();
        entity.setOrganizationId(change.getOrganizationId().value());
        entity.setChangedBy(change.getChangedBy().value());
        entity.setFromPlan(change.getFromPlan());
        entity.setToPlan(change.getToPlan());
        entity.setFromActiveUntil(change.getFromActiveUntil());
        entity.setToActiveUntil(change.getToActiveUntil());
        entity.setNote(change.getNote());
        entity.setIdempotencyKey(change.getIdempotencyKey().value());
        entity.setChangedAt(change.getChangedAt());
        return entity;
    }
}
