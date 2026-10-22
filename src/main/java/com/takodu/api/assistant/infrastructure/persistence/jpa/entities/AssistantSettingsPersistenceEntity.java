package com.takodu.api.assistant.infrastructure.persistence.jpa.entities;

import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "assistant_settings")
@Getter
@Setter
public class AssistantSettingsPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "business_info", nullable = false)
    private String businessInfo;
}
