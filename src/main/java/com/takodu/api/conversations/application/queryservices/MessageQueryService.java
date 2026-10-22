package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.queries.GetMessagesByConversationQuery;
import com.takodu.api.conversations.domain.model.queries.GetRecentMessagesQuery;

import java.util.List;

public interface MessageQueryService {

    /**
     * @return one page of the chat, newest first; reading it leaves the unread count alone (W8 is explicit)
     * @throws com.takodu.api.conversations.domain.exceptions.ConversationNotFoundException when the conversation is
     *         not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when staff are not assigned to its
     *         establishment
     */
    List<Message> handle(GetMessagesByConversationQuery query);

    /** @return the last messages, oldest first; empty when the conversation is not in the organization */
    List<Message> handle(GetRecentMessagesQuery query);
}
