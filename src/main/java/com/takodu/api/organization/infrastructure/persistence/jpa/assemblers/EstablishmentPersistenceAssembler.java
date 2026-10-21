package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.EstablishmentPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.key;
import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.value;

public final class EstablishmentPersistenceAssembler {
    private EstablishmentPersistenceAssembler() { }

    public static Establishment toDomainFromPersistence(EstablishmentPersistenceEntity entity) {
        return new Establishment(new EstablishmentId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                entity.getName(), entity.getAddress(), entity.getTimeZone(), entity.getArchivedAt(),
                key(entity.getIdempotencyKey()));
    }

    public static EstablishmentPersistenceEntity toPersistenceFromDomain(Establishment establishment) {
        var entity = new EstablishmentPersistenceEntity();
        entity.setId(establishment.getId() == null ? null : establishment.getId().value());
        entity.setOrganizationId(establishment.getOrganizationId().value());
        entity.setName(establishment.getName());
        entity.setAddress(establishment.getAddress());
        entity.setTimeZone(establishment.getTimeZone());
        entity.setArchivedAt(establishment.getArchivedAt());
        entity.setIdempotencyKey(value(establishment.getIdempotencyKey()));
        return entity;
    }
}
