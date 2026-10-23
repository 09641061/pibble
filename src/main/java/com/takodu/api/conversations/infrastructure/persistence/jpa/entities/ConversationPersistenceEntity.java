package com.takodu.api.conversations.infrastructure.persistence.jpa.entities;

import com.takodu.api.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class ConversationPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "line_id", nullable = false, updatable = false)
    private UUID lineId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "assigned_member_id")
    private UUID assignedMemberId;

    @Column(name = "bot_paused_until")
    private Instant botPausedUntil;

    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "assistant_handed_off_at")
    private Instant assistantHandedOffAt;

    @Column(name = "handoff_summary")
    private String handoffSummary;

    @Column(name = "assistant_manually_paused_at")
    private Instant manuallyPausedAt;

    @Column(name = "assistant_manually_paused_by")
    private UUID manuallyPausedBy;

    @Column(name = "last_idempotency_key")
    private UUID lastIdempotencyKey;
}
