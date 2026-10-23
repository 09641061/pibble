package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.OrganizationPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class OrganizationPersistenceAssembler {
    private OrganizationPersistenceAssembler() { }

    public static Organization toDomainFromPersistence(OrganizationPersistenceEntity entity) {
        return new Organization(new OrganizationId(entity.getId()), entity.getName(), entity.getPlan(),
                entity.getPlanActiveUntil(), key(entity.getIdempotencyKey()), key(entity.getLastIdempotencyKey()));
    }

    public static OrganizationPersistenceEntity toPersistenceFromDomain(Organization organization) {
        var entity = new OrganizationPersistenceEntity();
        entity.setId(organization.getId() == null ? null : organization.getId().value());
        entity.setName(organization.getName());
        entity.setPlan(organization.getPlan());
        entity.setPlanActiveUntil(organization.getPlanActiveUntil());
        entity.setIdempotencyKey(value(organization.getIdempotencyKey()));
        entity.setLastIdempotencyKey(value(organization.getLastIdempotencyKey()));
        return entity;
    }

    static IdempotencyKey key(UUID value) {
        return value == null ? null : new IdempotencyKey(value);
    }

    static UUID value(IdempotencyKey key) {
        return key == null ? null : key.value();
    }
}
