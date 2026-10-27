package com.takodu.api.shared.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Identity and audit columns shared by every persistence entity.
 *
 * <p>Primary keys are UUIDs generated on insert. Domain aggregates never extend this class; persistence
 * assemblers copy {@code id} into the aggregate's typed identifier, such as {@code CustomerId}.</p>
 */
@Getter
@MappedSuperclass
public abstract class AuditableAbstractPersistenceEntity {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Assemblers copy it back from the aggregate, so a merged entity keeps it; the column never changes. */
    @Setter
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
