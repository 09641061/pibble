package com.takodu.api.conversations.application.internal.queryservices;

import com.takodu.api.conversations.application.queryservices.MessageQueryService;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.queries.GetMessagesByConversationQuery;
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
    private final AccessPolicy accessPolicy;

    public MessageQueryServiceImpl(MessageRepository messageRepository, ConversationRepository conversationRepository,
                                   AccessPolicy accessPolicy) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
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
}
