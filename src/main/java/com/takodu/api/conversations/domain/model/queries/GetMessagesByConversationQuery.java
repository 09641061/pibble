package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * A page of the chat, newest first.
 *
 * @param before where the previous page ended; null for the newest messages
 */
public record GetMessagesByConversationQuery(OrganizationId organizationId, ConversationId conversationId,
                                             PageCursor before, int size) {
    public static final int MAX_SIZE = 100;

    public GetMessagesByConversationQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size must be between 1 and 100");
    }
}
