package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Something the inbox shows changed: a message arrived or was sent, a status moved, the chat was read or assigned.
 * Fired inside the transaction; the inbox stream forwards it after commit. Not an integration event. It carries the
 * conversation's summary as it was when fired, so the inbox can update its list without reloading the chat.
 *
 * @param windowClosesAt when free-form replies stop (W6); null when the customer never wrote
 * @param assistantPausedUntil until when the assistant stays quiet after a person wrote (A3)
 */
public record ConversationChangedEvent(OrganizationId organizationId, EstablishmentId establishmentId,
                                       ConversationId conversationId, Instant lastMessageAt, Instant lastInboundAt,
                                       int unreadCount, Instant windowClosesAt, Instant assistantPausedUntil,
                                       boolean assistantHandedOff, boolean assistantPausedManually)
        implements InboxEvent {

    /**
     * Instants are cut to microseconds, as PostgreSQL stores them, so the stream and a reload agree.
     *
     * @param conversation the conversation after its change, before it is saved or after
     */
    public static ConversationChangedEvent of(Conversation conversation) {
        return new ConversationChangedEvent(conversation.getOrganizationId(), conversation.getEstablishmentId(),
                conversation.getId(), micros(conversation.getLastMessageAt()), micros(conversation.getLastInboundAt()),
                conversation.getUnreadCount(), micros(conversation.windowClosesAt()),
                micros(conversation.getAssistantPausedUntil()), conversation.isHandedOff(),
                conversation.isManuallyPaused());
    }

    private static Instant micros(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MICROS);
    }
}
