package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.interfaces.rest.resources.MessagePageResource;
import com.takodu.api.conversations.interfaces.rest.resources.MessageResource;

import java.util.List;
import java.util.Locale;

public final class MessageResourceFromEntityAssembler {
    private MessageResourceFromEntityAssembler() { }

    public static MessageResource toResourceFromEntity(Message message) {
        var content = message.getContent();
        var choices = message.getChoices();
        return new MessageResource(message.getId().value(), message.getConversationId().value(),
                lower(message.getDirection()), lower(message.getOrigin()), lower(content.kind()), content.body(),
                content.mediaId() != null, content.mediaMime(),
                message.mediaStatus() == null ? null : lower(message.mediaStatus()), message.getTemplateName(), lower(message.getStatus()),
                message.getError(), message.getSentBy() == null ? null : message.getSentBy().value(),
                choices == null ? null : lower(choices.style()), choices == null ? List.of() : choices.titles(),
                message.getOccurredAt());
    }

    /** @param size the page size asked for; a full page may have older messages after it */
    public static MessagePageResource toPageFromEntities(List<Message> messages, int size) {
        var last = messages.isEmpty() ? null : messages.getLast();
        return new MessagePageResource(messages.stream().map(MessageResourceFromEntityAssembler::toResourceFromEntity).toList(),
                messages.size() < size ? null : PageCursorAssembler.toTokenFromCursor(last.getOccurredAt(),
                        last.getId().value()));
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
