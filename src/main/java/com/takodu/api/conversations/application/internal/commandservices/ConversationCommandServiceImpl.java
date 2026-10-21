package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.ConversationCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.commands.AssignConversationCommand;
import com.takodu.api.conversations.domain.model.commands.MarkConversationReadCommand;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ConversationCommandServiceImpl implements ConversationCommandService {
    private final ConversationRepository conversationRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;
    private final Event<ConversationChangedEvent> conversationChanged;

    public ConversationCommandServiceImpl(ConversationRepository conversationRepository,
                                          ExternalOrganizationService externalOrganizationService,
                                          AccessPolicy accessPolicy, Event<ConversationChangedEvent> conversationChanged) {
        this.conversationRepository = conversationRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
        this.conversationChanged = conversationChanged;
    }

    @Override
    @Transactional
    public Conversation handle(MarkConversationReadCommand command) {
        var conversation = lock(command.organizationId(), command.conversationId());
        conversation.markRead();
        return save(conversation);
    }

    @Override
    @Transactional
    public Conversation handle(AssignConversationCommand command) {
        var conversation = lock(command.organizationId(), command.conversationId());
        if (command.memberId() != null)
            externalOrganizationService.requireMemberWorksAt(command.organizationId(), command.memberId(),
                    conversation.getEstablishmentId());
        conversation.assignTo(command.memberId());
        return save(conversation);
    }

    /** Other members' inboxes follow the change (W8, W9). */
    private Conversation save(Conversation conversation) {
        var saved = conversationRepository.save(conversation);
        conversationChanged.fire(new ConversationChangedEvent(saved.getOrganizationId(), saved.getEstablishmentId(),
                saved.getId()));
        return saved;
    }

    /** Checks the caller before loading, then their establishment once the conversation names it (W7). */
    private Conversation lock(OrganizationId organizationId, ConversationId conversationId) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        var conversation = conversationRepository.lockById(organizationId, conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        accessPolicy.requireEstablishment(conversation.getEstablishmentId());
        return conversation;
    }
}
