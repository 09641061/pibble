package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.valueobjects.OpeningRange;
import com.takodu.api.organization.domain.model.valueobjects.WeeklySchedule;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.EstablishmentPersistenceEntity;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.OpeningRangePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.key;
import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.value;

public final class EstablishmentPersistenceAssembler {
    private EstablishmentPersistenceAssembler() { }

    public static Establishment toDomainFromPersistence(EstablishmentPersistenceEntity entity) {
        return new Establishment(new EstablishmentId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                entity.getName(), entity.getAddress(), entity.getTimeZone(), schedule(entity), entity.getArchivedAt(),
                key(entity.getIdempotencyKey()));
    }

    public static EstablishmentPersistenceEntity toPersistenceFromDomain(Establishment establishment) {
        var entity = new EstablishmentPersistenceEntity();
        entity.setId(establishment.getId() == null ? null : establishment.getId().value());
        entity.setOrganizationId(establishment.getOrganizationId().value());
        entity.setName(establishment.getName());
        entity.setAddress(establishment.getAddress());
        entity.setTimeZone(establishment.getTimeZone());
        entity.setOpeningHours(establishment.getSchedule().ranges().stream()
                .map(range -> new OpeningRangePersistenceEntity((short) range.day().getValue(), range.opensAt(),
                        range.closesAt()))
                .collect(Collectors.toCollection(ArrayList::new)));
        entity.setArchivedAt(establishment.getArchivedAt());
        entity.setIdempotencyKey(value(establishment.getIdempotencyKey()));
        return entity;
    }

    private static WeeklySchedule schedule(EstablishmentPersistenceEntity entity) {
        return new WeeklySchedule(entity.getOpeningHours().stream()
                .map(row -> new OpeningRange(DayOfWeek.of(row.getWeekday()), row.getOpensAt(), row.getClosesAt()))
                .toList());
    }
}
