package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.ServicePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Duration;
import java.util.HashSet;
import java.util.stream.Collectors;

import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.key;
import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.value;

public final class ServicePersistenceAssembler {
    private ServicePersistenceAssembler() { }

    public static Service toDomainFromPersistence(ServicePersistenceEntity entity) {
        return new Service(new ServiceId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), entity.getName(),
                Duration.ofMinutes(entity.getDurationMinutes()), new Money(entity.getPriceCents(), entity.getCurrency()),
                entity.getInstructions(), entity.isActive(),
                entity.getPerformerIds().stream().map(MemberId::new).collect(Collectors.toSet()),
                key(entity.getIdempotencyKey()));
    }

    public static ServicePersistenceEntity toPersistenceFromDomain(Service service) {
        var entity = new ServicePersistenceEntity();
        entity.setId(service.getId() == null ? null : service.getId().value());
        entity.setOrganizationId(service.getOrganizationId().value());
        entity.setEstablishmentId(service.getEstablishmentId().value());
        entity.setName(service.getName());
        entity.setDurationMinutes((int) service.getDuration().toMinutes());
        entity.setPriceCents(Math.toIntExact(service.getPrice().cents()));
        entity.setCurrency(service.getPrice().currency());
        entity.setInstructions(service.getInstructions());
        entity.setActive(service.isActive());
        entity.setIdempotencyKey(value(service.getIdempotencyKey()));
        entity.setPerformerIds(service.getPerformerIds().stream().map(MemberId::value)
                .collect(Collectors.toCollection(HashSet::new)));
        return entity;
    }
}
