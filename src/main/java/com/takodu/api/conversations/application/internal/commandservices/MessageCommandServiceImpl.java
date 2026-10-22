package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.MessageCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappGateway;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappSendException;
import com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException;
import com.takodu.api.conversations.domain.exceptions.LineDisconnectedException;
import com.takodu.api.conversations.domain.exceptions.WindowClosedException;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.SendAssistantReplyCommand;
import com.takodu.api.conversations.domain.model.commands.SendNextPendingMessageCommand;
import com.takodu.api.conversations.domain.model.commands.SendStaffMessageCommand;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.OutboundMessageQueuedEvent;
import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.Optional;

@ApplicationScoped
public class MessageCommandServiceImpl implements MessageCommandService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsappLineRepository whatsappLineRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalCustomersService externalCustomersService;
    private final WhatsappGateway whatsappGateway;
    private final AccessPolicy accessPolicy;
    private final Event<OutboundMessageQueuedEvent> outboundMessageQueued;
    private final Event<ConversationChangedEvent> conversationChanged;
    private final Clock clock;

    public MessageCommandServiceImpl(MessageRepository messageRepository, ConversationRepository conversationRepository,
                                     WhatsappLineRepository whatsappLineRepository,
                                     ExternalOrganizationService externalOrganizationService,
                                     ExternalCustomersService externalCustomersService, WhatsappGateway whatsappGateway,
                                     AccessPolicy accessPolicy, Event<OutboundMessageQueuedEvent> outboundMessageQueued,
                                     Event<ConversationChangedEvent> conversationChanged, Clock clock) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.whatsappLineRepository = whatsappLineRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalCustomersService = externalCustomersService;
        this.whatsappGateway = whatsappGateway;
        this.accessPolicy = accessPolicy;
        this.outboundMessageQueued = outboundMessageQueued;
        this.conversationChanged = conversationChanged;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Message handle(SendStaffMessageCommand command) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        var earlier = messageRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var conversation = conversationRepository.lockById(command.organizationId(), command.conversationId())
                .orElseThrow(() -> new ConversationNotFoundException(command.conversationId()));
        accessPolicy.requireEstablishment(conversation.getEstablishmentId());
        var establishment = externalOrganizationService.fetchEstablishment(command.organizationId(),
                conversation.getEstablishmentId());
        if (!establishment.planActive()) throw new PlanInactiveException(command.organizationId().value());
        if (!lineIsConnected(conversation)) throw new LineDisconnectedException();
        var now = clock.instant();
        if (!conversation.isWindowOpen(now)) throw new WindowClosedException();

        var message = messageRepository.save(Message.staffText(conversation, command.sentBy(), command.body(), now,
                command.idempotencyKey()));
        conversation.recordHumanOutbound(now);
        conversationRepository.save(conversation);
        outboundMessageQueued.fire(new OutboundMessageQueuedEvent(message.getId()));
        conversationChanged.fire(changed(conversation));
        return message;
    }

    @Override
    @Transactional
    public Optional<Message> handle(SendAssistantReplyCommand command) {
        var earlier = messageRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier;
        var conversation = conversationRepository.lockById(command.organizationId(), command.conversationId())
                .orElseThrow(() -> new ConversationNotFoundException(command.conversationId()));
        var now = clock.instant();
        var establishment = externalOrganizationService.fetchEstablishment(command.organizationId(),
                conversation.getEstablishmentId());
        if (!establishment.planActive() || !lineIsConnected(conversation) || !conversation.isWindowOpen(now)
                || conversation.isAssistantPaused(now)) return Optional.empty();

        var message = messageRepository.save(Message.assistantText(conversation, command.body(), now,
                command.idempotencyKey()));
        conversation.recordAssistantOutbound(now);
        conversationRepository.save(conversation);
        outboundMessageQueued.fire(new OutboundMessageQueuedEvent(message.getId()));
        conversationChanged.fire(changed(conversation));
        return Optional.of(message);
    }

    /** One row per transaction: the row stays locked while Meta answers, so no other sender takes it (takodu-jobs). */
    @Override
    @Transactional
    public boolean handle(SendNextPendingMessageCommand command) {
        var claimed = messageRepository.claimNextPending(command.now());
        if (claimed.isEmpty()) return false;
        var message = claimed.get();
        var conversation = conversationRepository.findById(message.getOrganizationId(), message.getConversationId())
                .orElseThrow();
        send(message, conversation, command);
        messageRepository.save(message);
        conversationChanged.fire(changed(conversation));
        return true;
    }

    private static ConversationChangedEvent changed(Conversation conversation) {
        return new ConversationChangedEvent(conversation.getOrganizationId(), conversation.getEstablishmentId(),
                conversation.getId());
    }

    /** Re-checks O6, W15 and W6 before calling Meta: they may have changed since the message was queued. */
    private void send(Message message, Conversation conversation, SendNextPendingMessageCommand command) {
        var line = whatsappLineRepository.findById(conversation.getOrganizationId(), conversation.getLineId()).orElseThrow();
        var establishment = externalOrganizationService.fetchEstablishment(conversation.getOrganizationId(),
                conversation.getEstablishmentId());
        var customer = externalCustomersService.fetchContact(conversation.getOrganizationId(),
                conversation.getCustomerId());
        if (!establishment.planActive()) {
            message.fail("The plan is inactive");
        } else if (!line.isConnected()) {
            message.fail(LineDisconnectedException.MESSAGE);
        } else if (message.getContent().kind() != MessageKind.TEMPLATE && !conversation.isWindowOpen(command.now())) {
            message.fail(WindowClosedException.MESSAGE);
        } else if (customer.isEmpty()) {
            message.fail("The customer no longer exists");
        } else {
            try {
                var to = customer.get().whatsappId();
                message.markSent(message.getContent().kind() == MessageKind.TEMPLATE
                        ? whatsappGateway.sendTemplate(line, to, message.getTemplateName(), NotificationTemplate.LANGUAGE,
                                message.getTemplateParameters())
                        : whatsappGateway.sendText(line, to, message.getContent().body()));
            } catch (WhatsappSendException e) {
                Log.warnf("Sending message %s failed: %s", message.getId().value(), e.getMessage());
                message.recordSendFailure(e.getMessage(), e.retryable(), command.now());
            }
        }
    }

    private boolean lineIsConnected(Conversation conversation) {
        return whatsappLineRepository.findById(conversation.getOrganizationId(), conversation.getLineId())
                .filter(WhatsappLine::isConnected).isPresent();
    }
}
