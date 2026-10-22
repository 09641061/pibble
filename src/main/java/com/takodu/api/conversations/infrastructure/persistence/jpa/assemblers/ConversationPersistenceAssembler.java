package com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.ConversationPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class ConversationPersistenceAssembler {
    private ConversationPersistenceAssembler() { }

    public static Conversation toDomainFromPersistence(ConversationPersistenceEntity entity) {
        return new Conversation(new ConversationId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), new WhatsappLineId(entity.getLineId()),
                new CustomerId(entity.getCustomerId()),
                entity.getAssignedMemberId() == null ? null : new MemberId(entity.getAssignedMemberId()),
                entity.getBotPausedUntil(), entity.getLastInboundAt(), entity.getLastMessageAt(), entity.getUnreadCount(),
                entity.getAssistantHandedOffAt(), entity.getHandoffSummary());
    }

    public static ConversationPersistenceEntity toPersistenceFromDomain(Conversation conversation) {
        var entity = new ConversationPersistenceEntity();
        entity.setId(conversation.getId() == null ? null : conversation.getId().value());
        entity.setOrganizationId(conversation.getOrganizationId().value());
        entity.setEstablishmentId(conversation.getEstablishmentId().value());
        entity.setLineId(conversation.getLineId().value());
        entity.setCustomerId(conversation.getCustomerId().value());
        entity.setAssignedMemberId(conversation.getAssignedMemberId() == null ? null
                : conversation.getAssignedMemberId().value());
        entity.setBotPausedUntil(conversation.getAssistantPausedUntil());
        entity.setLastInboundAt(conversation.getLastInboundAt());
        entity.setLastMessageAt(conversation.getLastMessageAt());
        entity.setUnreadCount(conversation.getUnreadCount());
        entity.setAssistantHandedOffAt(conversation.getAssistantHandedOffAt());
        entity.setHandoffSummary(conversation.getHandoffSummary());
        return entity;
    }
}
