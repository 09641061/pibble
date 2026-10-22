package com.takodu.api.conversations.infrastructure.persistence.jpa.entities;

import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.LineModePersistenceConverter;
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
@Table(name = "whatsapp_lines")
@Getter
@Setter
public class WhatsappLinePersistenceEntity extends AuditableAbstractPersistenceEntity {
    public static final String CONNECTED = "connected";
    public static final String DISCONNECTED = "disconnected";

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "waba_id", nullable = false)
    private String wabaId;

    @Column(name = "phone_number_id", nullable = false, updatable = false)
    private String phoneNumberId;

    @Column(name = "display_phone", nullable = false)
    private String displayPhone;

    @Column(name = "access_token_encrypted", nullable = false)
    private String accessTokenEncrypted;

    @Convert(converter = LineModePersistenceConverter.class)
    @Column(name = "mode", nullable = false)
    private LineMode mode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_idempotency_key")
    private UUID lastIdempotencyKey;
}
