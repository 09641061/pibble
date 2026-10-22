package com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.MessagePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class MessagePersistenceAssembler {
    private MessagePersistenceAssembler() { }

    public static Message toDomainFromPersistence(MessagePersistenceEntity entity) {
        return new Message(new MessageId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new ConversationId(entity.getConversationId()), entity.getDirection(), entity.getOrigin(),
                entity.getWamid() == null ? null : new Wamid(entity.getWamid()),
                new MessageContent(entity.getKind(), entity.getBody(), entity.getMediaId(), entity.getMediaMime()),
                entity.getTemplateName(), entity.getStatus(), entity.getError(), entity.getPricingCategory(),
                entity.getBillable(), entity.getSentByMemberId() == null ? null : new MemberId(entity.getSentByMemberId()),
                entity.getAttempts(), entity.getNextAttemptAt(),
                entity.getIdempotencyKey() == null ? null : new IdempotencyKey(entity.getIdempotencyKey()),
                entity.getOccurredAt());
    }

    public static MessagePersistenceEntity toPersistenceFromDomain(Message message) {
        var entity = new MessagePersistenceEntity();
        entity.setId(message.getId() == null ? null : message.getId().value());
        entity.setOrganizationId(message.getOrganizationId().value());
        entity.setConversationId(message.getConversationId().value());
        entity.setDirection(message.getDirection());
        entity.setOrigin(message.getOrigin());
        entity.setWamid(message.getWamid() == null ? null : message.getWamid().value());
        entity.setKind(message.getContent().kind());
        entity.setBody(message.getContent().body());
        entity.setMediaId(message.getContent().mediaId());
        entity.setMediaMime(message.getContent().mediaMime());
        entity.setTemplateName(message.getTemplateName());
        entity.setStatus(message.getStatus());
        entity.setError(message.getError());
        entity.setPricingCategory(message.getPricingCategory());
        entity.setBillable(message.getBillable());
        entity.setSentByMemberId(message.getSentBy() == null ? null : message.getSentBy().value());
        entity.setAttempts((short) message.getAttempts());
        entity.setNextAttemptAt(message.getNextAttemptAt());
        entity.setIdempotencyKey(message.getIdempotencyKey() == null ? null : message.getIdempotencyKey().value());
        entity.setOccurredAt(message.getOccurredAt());
        return entity;
    }
}
