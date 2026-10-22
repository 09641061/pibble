package com.takodu.api.conversations.infrastructure.persistence.jpa.entities;

import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageDirection;
import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import com.takodu.api.conversations.domain.model.valueobjects.MessageOrigin;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.DeliveryStatusPersistenceConverter;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.MessageDirectionPersistenceConverter;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.MessageKindPersistenceConverter;
import com.takodu.api.conversations.infrastructure.persistence.jpa.converters.MessageOriginPersistenceConverter;
import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class MessagePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Convert(converter = MessageDirectionPersistenceConverter.class)
    @Column(name = "direction", nullable = false, updatable = false)
    private MessageDirection direction;

    @Convert(converter = MessageOriginPersistenceConverter.class)
    @Column(name = "origin", nullable = false, updatable = false)
    private MessageOrigin origin;

    @Column(name = "wamid")
    private String wamid;

    @Convert(converter = MessageKindPersistenceConverter.class)
    @Column(name = "kind", nullable = false, updatable = false)
    private MessageKind kind;

    @Column(name = "body", updatable = false)
    private String body;

    @Column(name = "media_id", updatable = false)
    private String mediaId;

    @Column(name = "media_mime", updatable = false)
    private String mediaMime;

    @Column(name = "template_name", updatable = false)
    private String templateName;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "template_parameters", updatable = false)
    private String[] templateParameters;

    @Convert(converter = DeliveryStatusPersistenceConverter.class)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "error")
    private String error;

    @Column(name = "pricing_category")
    private String pricingCategory;

    @Column(name = "billable")
    private Boolean billable;

    @Column(name = "sent_by_member_id", updatable = false)
    private UUID sentByMemberId;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
