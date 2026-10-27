package com.takodu.api.conversations.application.internal.queryservices;

import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.application.queryservices.AssistantConversation;
import com.takodu.api.conversations.application.queryservices.ConversationQueryService;
import com.takodu.api.conversations.application.queryservices.InboxConversation;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.queries.GetConversationByIdQuery;
import com.takodu.api.conversations.domain.model.queries.GetConversationForAssistantQuery;
import com.takodu.api.conversations.domain.model.queries.GetConversationsByEstablishmentQuery;
import com.takodu.api.conversations.domain.model.queries.GetInboxSummaryQuery;
import com.takodu.api.conversations.domain.model.valueobjects.InboxSummary;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ConversationQueryServiceImpl implements ConversationQueryService {
    private final ConversationRepository conversationRepository;
    private final WhatsappLineRepository whatsappLineRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalCustomersService externalCustomersService;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public ConversationQueryServiceImpl(ConversationRepository conversationRepository,
                                        WhatsappLineRepository whatsappLineRepository,
                                        ExternalOrganizationService externalOrganizationService,
                                        ExternalCustomersService externalCustomersService, AccessPolicy accessPolicy,
                                        Clock clock) {
        this.conversationRepository = conversationRepository;
        this.whatsappLineRepository = whatsappLineRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalCustomersService = externalCustomersService;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public List<InboxConversation> handle(GetConversationsByEstablishmentQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        externalOrganizationService.fetchEstablishment(query.organizationId(), query.establishmentId());
        accessPolicy.requireEstablishment(query.establishmentId());
        var conversations = conversationRepository.findByEstablishment(query.organizationId(), query.establishmentId(),
                query.after(), query.size());
        return inbox(query.organizationId(), conversations);
    }

    @Override
    public InboxSummary handle(GetInboxSummaryQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        externalOrganizationService.fetchEstablishment(query.organizationId(), query.establishmentId());
        accessPolicy.requireEstablishment(query.establishmentId());
        return conversationRepository.summarize(query.organizationId(), query.establishmentId());
    }

    @Override
    public Optional<InboxConversation> handle(GetConversationByIdQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        var conversation = conversationRepository.findById(query.organizationId(), query.conversationId());
        conversation.ifPresent(found -> accessPolicy.requireEstablishment(found.getEstablishmentId()));
        return conversation.map(found -> inbox(query.organizationId(), List.of(found)).getFirst());
    }

    @Override
    public Optional<AssistantConversation> handle(GetConversationForAssistantQuery query) {
        return conversationRepository.findById(query.organizationId(), query.conversationId())
                .map(conversation -> new AssistantConversation(conversation, whatsappLineRepository
                        .findById(query.organizationId(), conversation.getLineId())
                        .filter(WhatsappLine::isConnected).isPresent()));
    }

    /** One call to customers for the whole page. */
    private List<InboxConversation> inbox(OrganizationId organizationId, List<Conversation> conversations) {
        var now = clock.instant();
        var customers = externalCustomersService.fetchContacts(organizationId,
                conversations.stream().map(Conversation::getCustomerId).toList());
        return conversations.stream()
                .map(conversation -> new InboxConversation(conversation, customers.get(conversation.getCustomerId()),
                        conversation.isWindowOpen(now), conversation.isAssistantPaused(now)))
                .toList();
    }
}
