package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * @param type {@code conversation} when a conversation changed; {@code assistant_typing} when the assistant started
 *             answering it and {@code assistant_idle} when it finished, whatever the outcome;
 *             {@code appointment_booked}, {@code appointment_rescheduled} or {@code appointment_cancelled} when an
 *             appointment changed; {@code ping} right after connecting and every 25 seconds, so proxies keep the
 *             stream open
 * @param conversationId null on appointment events and pings
 * @param conversation the conversation's summary after the change; only on {@code conversation} events
 * @param appointmentId only on appointment events
 */
@Schema(name = "InboxChange")
public record InboxChangeResource(String type, UUID conversationId, UUID establishmentId,
                                  InboxConversationSummaryResource conversation, UUID appointmentId) {
    public static InboxChangeResource ping() {
        return new InboxChangeResource("ping", null, null, null, null);
    }
}
