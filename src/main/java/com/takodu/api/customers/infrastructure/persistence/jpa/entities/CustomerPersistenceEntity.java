package com.takodu.api.customers.infrastructure.persistence.jpa.entities;

import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.infrastructure.persistence.jpa.converters.DocumentTypePersistenceConverter;
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
@Table(name = "customers")
@Getter
@Setter
public class CustomerPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "phone_e164", nullable = false, updatable = false)
    private String phoneE164;

    @Column(name = "name")
    private String name;

    @Column(name = "whatsapp_profile_name")
    private String whatsappProfileName;

    @Convert(converter = DocumentTypePersistenceConverter.class)
    @Column(name = "document_type")
    private IdentityDocument.Type documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
