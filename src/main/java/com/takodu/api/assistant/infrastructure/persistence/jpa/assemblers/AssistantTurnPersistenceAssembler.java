package com.takodu.api.assistant.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantTurnId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;
import com.takodu.api.assistant.infrastructure.persistence.jpa.entities.AssistantTurnPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class AssistantTurnPersistenceAssembler {
    private AssistantTurnPersistenceAssembler() { }

    public static AssistantTurn toDomainFromPersistence(AssistantTurnPersistenceEntity entity) {
        var usage = entity.getModel() == null ? null : new TurnUsage(entity.getModel(), orZero(entity.getInputTokens()),
                orZero(entity.getCachedTokens()), orZero(entity.getOutputTokens()));
        return new AssistantTurn(new AssistantTurnId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), new ConversationId(entity.getConversationId()),
                new CustomerId(entity.getCustomerId()), new MessageId(entity.getTriggerMessageId()),
                entity.getTriggerKind(), entity.getTriggerAt(), entity.getStatus(), entity.getAttempts(),
                entity.getRunAfter(), entity.getStartedAt(), entity.getAnsweredThrough(), usage, entity.getToolCalls(),
                entity.getError(), entity.getFinishedAt());
    }

    public static AssistantTurnPersistenceEntity toPersistenceFromDomain(AssistantTurn turn) {
        var entity = new AssistantTurnPersistenceEntity();
        entity.setId(turn.getId() == null ? null : turn.getId().value());
        entity.setOrganizationId(turn.getOrganizationId().value());
        entity.setEstablishmentId(turn.getEstablishmentId().value());
        entity.setConversationId(turn.getConversationId().value());
        entity.setCustomerId(turn.getCustomerId().value());
        entity.setTriggerMessageId(turn.getTriggerMessageId().value());
        entity.setTriggerKind(turn.getTriggerKind());
        entity.setTriggerAt(turn.getTriggerAt());
        entity.setStatus(turn.getStatus());
        entity.setAttempts((short) turn.getAttempts());
        entity.setRunAfter(turn.getRunAfter());
        entity.setStartedAt(turn.getStartedAt());
        entity.setAnsweredThrough(turn.getAnsweredThrough());
        var usage = turn.getUsage();
        entity.setModel(usage == null ? null : usage.model());
        entity.setInputTokens(usage == null ? null : usage.inputTokens());
        entity.setCachedTokens(usage == null ? null : usage.cachedTokens());
        entity.setOutputTokens(usage == null ? null : usage.outputTokens());
        entity.setToolCalls((short) turn.getToolCalls());
        entity.setError(turn.getError());
        entity.setFinishedAt(turn.getFinishedAt());
        return entity;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
