package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public record GetConversationByIdQuery(OrganizationId organizationId, ConversationId conversationId) {
    public GetConversationByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
    }
}
