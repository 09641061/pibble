package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param summary what the customer needs, for the person who takes the chat */
public record HandOffConversationCommand(OrganizationId organizationId, ConversationId conversationId,
                                         String summary) {
    public HandOffConversationCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
    }
}
