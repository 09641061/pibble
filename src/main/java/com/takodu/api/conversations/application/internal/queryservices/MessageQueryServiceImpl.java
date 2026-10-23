package com.takodu.api.conversations.application.internal.queryservices;

import com.takodu.api.conversations.application.internal.outboundservices.media.MediaStorage;
import com.takodu.api.conversations.application.queryservices.MessageMedia;
import com.takodu.api.conversations.application.queryservices.MessageQueryService;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.MediaDownloadFailedException;
import com.takodu.api.conversations.domain.exceptions.MediaNotReadyException;
import com.takodu.api.conversations.domain.exceptions.MediaRemovedException;
import com.takodu.api.conversations.domain.exceptions.MessageNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.queries.GetMessageMediaQuery;
import com.takodu.api.conversations.domain.model.queries.GetMessagesByConversationQuery;
import com.takodu.api.conversations.domain.model.queries.GetRecentMessagesQuery;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MessageQueryServiceImpl implements MessageQueryService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MediaStorage mediaStorage;
    private final AccessPolicy accessPolicy;

    public MessageQueryServiceImpl(MessageRepository messageRepository, ConversationRepository conversationRepository,
                                   MediaStorage mediaStorage, AccessPolicy accessPolicy) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.mediaStorage = mediaStorage;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<Message> handle(GetMessagesByConversationQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        var conversation = conversationRepository.findById(query.organizationId(), query.conversationId())
                .orElseThrow(() -> new ConversationNotFoundException(query.conversationId()));
        accessPolicy.requireEstablishment(conversation.getEstablishmentId());
        return messageRepository.findByConversation(query.organizationId(), query.conversationId(), query.before(),
                query.size());
    }

    @Override
    public MessageMedia handle(GetMessageMediaQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        var message = messageRepository.findById(query.organizationId(), query.messageId())
                .filter(found -> found.getMedia() != null)
                .orElseThrow(() -> new MessageNotFoundException(query.messageId()));
        var conversation = conversationRepository.findById(query.organizationId(), message.getConversationId())
                .orElseThrow(() -> new MessageNotFoundException(query.messageId()));
        accessPolicy.requireEstablishment(conversation.getEstablishmentId());
        var media = message.getMedia();
        return switch (media.status()) {
            case PENDING -> throw new MediaNotReadyException();
            case FAILED -> throw new MediaDownloadFailedException(media.error());
            case READY -> new MessageMedia(message.getContent().mediaMime(), mediaStorage.open(media.path()));
            case REMOVING, REMOVED -> throw new MediaRemovedException();
        };
    }

    @Override
    public List<Message> handle(GetRecentMessagesQuery query) {
        return messageRepository.findByConversation(query.organizationId(), query.conversationId(), null,
                query.limit()).reversed();
    }
}
