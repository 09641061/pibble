package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param idempotencyKey derived from the assistant's turn, so a retried turn queues one reply */
public record SendAssistantReplyCommand(OrganizationId organizationId, ConversationId conversationId, String body,
                                        IdempotencyKey idempotencyKey) {
    public SendAssistantReplyCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("idempotencyKey is required");
    }
}
