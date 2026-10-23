package com.takodu.api.conversations.application.internal.commandservices;

import com.takodu.api.conversations.application.commandservices.MediaCommandService;
import com.takodu.api.conversations.application.internal.outboundservices.media.MediaStorage;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaException;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaGateway;
import com.takodu.api.conversations.domain.exceptions.LineDisconnectedException;
import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.commands.DownloadNextMediaCommand;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.valueobjects.MediaDownload;
import com.takodu.api.conversations.domain.repositories.ConversationRepository;
import com.takodu.api.conversations.domain.repositories.MessageRepository;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

@ApplicationScoped
public class MediaCommandServiceImpl implements MediaCommandService {
    static final String NOT_STORED = "Takodu could not store the file";

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsappLineRepository whatsappLineRepository;
    private final WhatsappMediaGateway whatsappMediaGateway;
    private final MediaStorage mediaStorage;
    private final Event<ConversationChangedEvent> conversationChanged;

    public MediaCommandServiceImpl(MessageRepository messageRepository, ConversationRepository conversationRepository,
                                   WhatsappLineRepository whatsappLineRepository,
                                   WhatsappMediaGateway whatsappMediaGateway, MediaStorage mediaStorage,
                                   Event<ConversationChangedEvent> conversationChanged) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.whatsappLineRepository = whatsappLineRepository;
        this.whatsappMediaGateway = whatsappMediaGateway;
        this.mediaStorage = mediaStorage;
        this.conversationChanged = conversationChanged;
    }

    /** One message per transaction, locked while the file downloads, so no other job takes it (takodu-jobs). */
    @Override
    @Transactional
    public boolean handle(DownloadNextMediaCommand command) {
        var claimed = messageRepository.claimNextMediaDue(command.now());
        if (claimed.isEmpty()) return false;
        var message = claimed.get();
        var conversation = conversationRepository.findById(message.getOrganizationId(), message.getConversationId())
                .orElseThrow();
        var line = whatsappLineRepository.findById(conversation.getOrganizationId(), conversation.getLineId())
                .filter(WhatsappLine::isConnected);
        if (line.isEmpty()) {
            message.recordMediaFailure(LineDisconnectedException.MESSAGE, true, command.now());
        } else {
            download(message, line.get(), command.now());
        }
        messageRepository.save(message);
        conversationChanged.fire(new ConversationChangedEvent(conversation.getOrganizationId(),
                conversation.getEstablishmentId(), conversation.getId()));
        return true;
    }

    /** Logs the message id and Meta's reason only: never the url, the token, the number or the content. */
    private void download(Message message, WhatsappLine line, Instant now) {
        var maxBytes = MediaDownload.maxBytes(message.getContent().kind());
        try (var content = whatsappMediaGateway.download(line, message.getContent().mediaId(), maxBytes)) {
            message.recordMediaStored(mediaStorage.store(message.getOrganizationId(), message.getId(), content));
        } catch (WhatsappMediaException e) {
            Log.warnf("Downloading the media of message %s failed: %s", message.getId().value(), e.getMessage());
            message.recordMediaFailure(e.getMessage(), e.retryable(), now);
        } catch (IOException | UncheckedIOException e) {
            Log.warnf("Storing the media of message %s failed: %s", message.getId().value(),
                    e.getClass().getSimpleName());
            message.recordMediaFailure(NOT_STORED, true, now);
        }
    }
}
