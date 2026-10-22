package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * The last messages of a chat, oldest first, for the assistant's memory. Answered without checking the caller.
 *
 * @param limit at most {@link GetMessagesByConversationQuery#MAX_SIZE}
 */
public record GetRecentMessagesQuery(OrganizationId organizationId, ConversationId conversationId, int limit) {
    public GetRecentMessagesQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (limit < 1 || limit > GetMessagesByConversationQuery.MAX_SIZE)
            throw new IllegalArgumentException("limit must be between 1 and 100");
    }
}
