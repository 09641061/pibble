package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.WhatsappWebhookCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalCustomersService;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappWebhookReader;
import com.takodu.api.conversations.domain.exceptions.InvalidWebhookSignatureException;
import com.takodu.api.conversations.domain.exceptions.WebhookVerificationFailedException;
import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.DisconnectAccountLinesCommand;
import com.takodu.api.conversations.domain.model.commands.ReceiveWhatsappWebhookCommand;
import com.takodu.api.conversations.domain.model.commands.RecordBusinessAppEchoCommand;
import com.takodu.api.conversations.domain.model.commands.RecordDeliveryStatusCommand;
import com.takodu.api.conversations.domain.model.commands.RecordHistoryMessageCommand;
import com.takodu.api.conversations.domain.model.commands.RecordInboundMessageCommand;
import com.takodu.api.conversations.domain.model.commands.RecordTemplateStatusCommand;
import com.takodu.api.conversations.domain.model.commands.VerifyWhatsappWebhookCommand;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.events.MediaReceivedEvent;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.conversations.interfaces.events.InboundMessageReceivedIntegrationEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class WhatsappWebhookCommandServiceImpl implements WhatsappWebhookCommandService {
    private final WhatsappWebhookReader whatsappWebhookReader;
    private final WhatsappLineRepository whatsappLineRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ExternalCustomersService externalCustomersService;
    private final Event<InboundMessageReceivedIntegrationEvent> inboundMessageReceived;
    private final Event<ConversationChangedEvent> conversationChanged;
    private final Event<MediaReceivedEvent> mediaReceived;

    public WhatsappWebhookCommandServiceImpl(WhatsappWebhookReader whatsappWebhookReader,
                                             WhatsappLineRepository whatsappLineRepository,
                                             ConversationRepository conversationRepository,
                                             MessageRepository messageRepository,
                                             ExternalCustomersService externalCustomersService,
                                             Event<InboundMessageReceivedIntegrationEvent> inboundMessageReceived,
                                             Event<ConversationChangedEvent> conversationChanged,
                                             Event<MediaReceivedEvent> mediaReceived) {
        this.whatsappWebhookReader = whatsappWebhookReader;
        this.whatsappLineRepository = whatsappLineRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.externalCustomersService = externalCustomersService;
        this.inboundMessageReceived = inboundMessageReceived;
        this.conversationChanged = conversationChanged;
        this.mediaReceived = mediaReceived;
    }

    @Override
    @Transactional
    public void handle(ReceiveWhatsappWebhookCommand command) {
        if (!whatsappWebhookReader.isAuthentic(command.body(), command.signature()))
            throw new InvalidWebhookSignatureException();
        var changed = new LinkedHashMap<ConversationId, ConversationChangedEvent>();
        for (var fact : whatsappWebhookReader.read(command.body())) {
            var conversation = switch (fact) {
                case RecordInboundMessageCommand inbound -> record(inbound);
                case RecordBusinessAppEchoCommand echo -> record(echo);
                case RecordHistoryMessageCommand history -> record(history);
                case RecordDeliveryStatusCommand status -> record(status);
                case DisconnectAccountLinesCommand disconnect -> record(disconnect);
                case RecordTemplateStatusCommand template -> record(template);
            };
            // The last fact of a conversation wins, so its summary is the one the transaction commits.
            conversation.ifPresent(found -> changed.put(found.getId(), ConversationChangedEvent.of(found)));
        }
        changed.values().forEach(conversationChanged::fire);
    }

    @Override
    public String handle(VerifyWhatsappWebhookCommand command) {
        if (!"subscribe".equals(command.mode()) || !whatsappWebhookReader.acceptsVerifyToken(command.verifyToken()))
            throw new WebhookVerificationFailedException();
        return command.challenge();
    }

    /** W3, W4, W6: the customer wrote; published to the assistant only the first time the wamid arrives. */
    private Optional<Conversation> record(RecordInboundMessageCommand command) {
        var line = connectedLine(command.phoneNumberId());
        if (line.isEmpty()) return Optional.empty();
        var customerId = externalCustomersService.findOrCreateByWhatsapp(line.get().getOrganizationId(),
                command.customerWaId(), command.profileName());
        var conversation = conversationRepository.lockOrStart(line.get(), customerId, command.occurredAt());
        if (messageRepository.existsByWamid(command.wamid())) return Optional.empty();
        var message = store(Message.inbound(conversation, command.wamid(), command.content(), command.occurredAt()));
        conversation.recordInbound(command.occurredAt());
        conversationRepository.save(conversation);
        inboundMessageReceived.fire(new InboundMessageReceivedIntegrationEvent(conversation.getOrganizationId().value(),
                message.getId().value(), conversation.getId().value(), conversation.getEstablishmentId().value(),
                conversation.getCustomerId().value(), command.content().kind().name().toLowerCase(Locale.ROOT),
                command.occurredAt()));
        return Optional.of(conversation);
    }

    /** W5, A3: the owner answered from the phone, so the chat shows it and the assistant steps back. */
    private Optional<Conversation> record(RecordBusinessAppEchoCommand command) {
        var conversation = conversationOf(command.phoneNumberId(), command.customerWaId(), command.occurredAt());
        if (conversation.isEmpty() || messageRepository.existsByWamid(command.wamid())) return Optional.empty();
        store(Message.businessAppEcho(conversation.get(), command.wamid(), command.content(), command.occurredAt()));
        conversation.get().recordHumanOutbound(command.occurredAt());
        return Optional.of(conversationRepository.save(conversation.get()));
    }

    /** History fills the chat but opens no window, counts as read and wakes no assistant. */
    private Optional<Conversation> record(RecordHistoryMessageCommand command) {
        var conversation = conversationOf(command.phoneNumberId(), command.customerWaId(), command.occurredAt());
        if (conversation.isEmpty() || messageRepository.existsByWamid(command.wamid())) return Optional.empty();
        store(Message.history(conversation.get(), command.wamid(), command.content(),
                command.fromCustomer(), command.status(), command.occurredAt()));
        conversation.get().recordHistory(command.occurredAt());
        return Optional.of(conversationRepository.save(conversation.get()));
    }

    /** Media is downloaded later by the media job, never in the webhook (07 §4). */
    private Message store(Message message) {
        var stored = messageRepository.save(message);
        if (stored.getMedia() != null) mediaReceived.fire(new MediaReceivedEvent(stored.getId()));
        return stored;
    }

    /** W10: statuses for messages Takodu does not know, such as ones sent before the line connected, are ignored. */
    private Optional<Conversation> record(RecordDeliveryStatusCommand command) {
        var message = messageRepository.lockByWamid(command.wamid());
        if (message.isEmpty()) return Optional.empty();
        message.get().applyStatus(command.status(), command.pricingCategory(), command.billable(), command.error());
        messageRepository.save(message.get());
        return conversationRepository.findById(message.get().getOrganizationId(), message.get().getConversationId());
    }

    private Optional<Conversation> record(DisconnectAccountLinesCommand command) {
        for (var line : whatsappLineRepository.findConnectedByWabaId(command.wabaId())) {
            line.disconnect();
            whatsappLineRepository.save(line);
            Log.infof("WhatsApp line %s disconnected by Meta", line.getId().value());
        }
        return Optional.empty();
    }

    /** Templates belong to the account, so every connected line of it learns the new status. */
    private Optional<Conversation> record(RecordTemplateStatusCommand command) {
        for (var line : whatsappLineRepository.findConnectedByWabaId(command.wabaId())) {
            line.recordTemplateStatus(command.template(), command.status());
            whatsappLineRepository.save(line);
            Log.infof("Template %s is %s on WhatsApp line %s", command.template().metaName(), command.status(),
                    line.getId().value());
        }
        return Optional.empty();
    }

    private Optional<Conversation> conversationOf(String phoneNumberId, String customerWaId, Instant firstMessageAt) {
        return connectedLine(phoneNumberId).map(line -> conversationRepository.lockOrStart(line,
                externalCustomersService.findOrCreateByWhatsapp(line.getOrganizationId(), customerWaId, null),
                firstMessageAt));
    }

    /** Payloads are untrusted: a number Takodu does not know, or one disconnected, is acknowledged and ignored. */
    private Optional<WhatsappLine> connectedLine(String phoneNumberId) {
        return whatsappLineRepository.findByPhoneNumberId(phoneNumberId).filter(WhatsappLine::isConnected);
    }
}
