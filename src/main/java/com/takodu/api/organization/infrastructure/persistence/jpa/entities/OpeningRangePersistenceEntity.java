package com.takodu.api.organization.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** One row of {@code opening_hours}, embedded in {@link EstablishmentPersistenceEntity}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class OpeningRangePersistenceEntity {
    @Column(name = "weekday", nullable = false)
    private short weekday;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    public OpeningRangePersistenceEntity(short weekday, LocalTime opensAt, LocalTime closesAt) {
        this.weekday = weekday;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }
}
