package com.takodu.api.organization.infrastructure.persistence.jpa.entities;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.organization.infrastructure.persistence.jpa.converters.IanaZonePersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "establishments")
@Getter
@Setter
public class EstablishmentPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Convert(converter = IanaZonePersistenceConverter.class)
    @Column(name = "time_zone", nullable = false)
    private IanaZone timeZone;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "opening_hours", joinColumns = @JoinColumn(name = "establishment_id"))
    private List<OpeningRangePersistenceEntity> openingHours = new ArrayList<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;
}
