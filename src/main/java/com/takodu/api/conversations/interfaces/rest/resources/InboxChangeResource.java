package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * @param type {@code conversation} when a conversation changed; {@code ping} right after connecting and every 25
 *             seconds, so proxies keep the stream open
 */
@Schema(name = "InboxChange")
public record InboxChangeResource(String type, UUID conversationId, UUID establishmentId) {
    public static InboxChangeResource ping() {
        return new InboxChangeResource("ping", null, null);
    }
}
