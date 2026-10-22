package com.takodu.api.assistant.infrastructure.persistence.jpa.entities;

import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import com.takodu.api.assistant.infrastructure.persistence.jpa.converters.TurnStatusPersistenceConverter;
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
@Table(name = "assistant_turns")
@Getter
@Setter
public class AssistantTurnPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "trigger_message_id", nullable = false, updatable = false)
    private UUID triggerMessageId;

    @Column(name = "trigger_kind", nullable = false, updatable = false)
    private String triggerKind;

    @Column(name = "trigger_at", nullable = false, updatable = false)
    private Instant triggerAt;

    @Convert(converter = TurnStatusPersistenceConverter.class)
    @Column(name = "status", nullable = false)
    private TurnStatus status;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "run_after", nullable = false)
    private Instant runAfter;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "answered_through")
    private Instant answeredThrough;

    @Column(name = "model")
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "cached_tokens")
    private Integer cachedTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "tool_calls", nullable = false)
    private short toolCalls;

    @Column(name = "error")
    private String error;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
