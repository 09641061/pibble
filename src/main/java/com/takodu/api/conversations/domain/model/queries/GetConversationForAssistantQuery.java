package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** The conversation an assistant turn answers in. Answered without checking the caller. */
public record GetConversationForAssistantQuery(OrganizationId organizationId, ConversationId conversationId) {
    public GetConversationForAssistantQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
    }
}
