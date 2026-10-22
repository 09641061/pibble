package com.takodu.api.conversations.infrastructure.persistence.jpa.entities;

import com.takodu.api.conversations.domain.model.valueobjects.NotificationKind;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationStatus;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.NotificationKindPersistenceConverter;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.NotificationStatusPersistenceConverter;
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
@Table(name = "scheduled_messages")
@Getter
@Setter
public class ScheduledMessagePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Convert(converter = NotificationKindPersistenceConverter.class)
    @Column(name = "kind", nullable = false, updatable = false)
    private NotificationKind kind;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "send_at", nullable = false)
    private Instant sendAt;

    @Convert(converter = NotificationStatusPersistenceConverter.class)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "cancel_reason", updatable = false)
    private String cancelReason;

    @Column(name = "message_id")
    private UUID messageId;
}
