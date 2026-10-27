package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * @param type {@code conversation} when a conversation changed; {@code assistant_typing} when the assistant started
 *             answering it and {@code assistant_idle} when it finished, whatever the outcome; {@code ping} right
 *             after connecting and every 25 seconds, so proxies keep the stream open
 * @param conversation the conversation's summary after the change; only on {@code conversation} events
 */
@Schema(name = "InboxChange")
public record InboxChangeResource(String type, UUID conversationId, UUID establishmentId,
                                  InboxConversationSummaryResource conversation) {
    public static InboxChangeResource ping() {
        return new InboxChangeResource("ping", null, null, null);
    }
}
