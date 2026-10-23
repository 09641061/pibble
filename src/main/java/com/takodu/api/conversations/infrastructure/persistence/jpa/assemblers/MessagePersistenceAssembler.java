package com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.InteractiveChoices;
import com.takodu.api.conversations.domain.model.valueobjects.MediaDownload;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.MessagePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.List;
import java.util.Locale;

public final class MessagePersistenceAssembler {
    private MessagePersistenceAssembler() { }

    public static Message toDomainFromPersistence(MessagePersistenceEntity entity) {
        return new Message(new MessageId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new ConversationId(entity.getConversationId()), entity.getDirection(), entity.getOrigin(),
                entity.getWamid() == null ? null : new Wamid(entity.getWamid()),
                new MessageContent(entity.getKind(), entity.getBody(), entity.getMediaId(), entity.getMediaMime()),
                entity.getTemplateName(),
                entity.getTemplateParameters() == null ? List.of() : List.of(entity.getTemplateParameters()),
                entity.getInteractiveStyle() == null ? null : new InteractiveChoices(
                        InteractiveChoices.Style.valueOf(entity.getInteractiveStyle().toUpperCase(Locale.ROOT)),
                        List.of(entity.getInteractiveOptions())),
                entity.getStatus(), entity.getError(), entity.getPricingCategory(),
                entity.getBillable(), entity.getSentByMemberId() == null ? null : new MemberId(entity.getSentByMemberId()),
                entity.getAttempts(), entity.getNextAttemptAt(),
                entity.getIdempotencyKey() == null ? null : new IdempotencyKey(entity.getIdempotencyKey()),
                entity.getOccurredAt(),
                entity.getMediaStatus() == null ? null : new MediaDownload(entity.getMediaStatus(),
                        entity.getMediaPath(), entity.getMediaAttempts(), entity.getMediaNextAttemptAt(),
                        entity.getMediaError()));
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
        var media = message.getMedia();
        if (media != null) {
            entity.setMediaStatus(media.status());
            entity.setMediaPath(media.path());
            entity.setMediaAttempts((short) media.attempts());
            entity.setMediaNextAttemptAt(media.nextAttemptAt());
            entity.setMediaError(media.error());
        }
        entity.setTemplateName(message.getTemplateName());
        entity.setTemplateParameters(message.getTemplateName() == null ? null
                : message.getTemplateParameters().toArray(String[]::new));
        var choices = message.getChoices();
        entity.setInteractiveStyle(choices == null ? null : choices.style().name().toLowerCase(Locale.ROOT));
        entity.setInteractiveOptions(choices == null ? null : choices.titles().toArray(String[]::new));
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
