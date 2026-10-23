package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.queries.GetMessageMediaQuery;
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

    /**
     * 07 §4: the downloaded file of a message's media. Works while the plan is inactive.
     *
     * @throws com.takodu.api.conversations.domain.exceptions.MessageNotFoundException when the message is not in the
     *         organization or carries no media
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when staff are not assigned to its
     *         establishment
     * @throws com.takodu.api.conversations.domain.exceptions.MediaNotReadyException while it is being downloaded
     * @throws com.takodu.api.conversations.domain.exceptions.MediaDownloadFailedException when it could not be
     *         downloaded
     */
    MessageMedia handle(GetMessageMediaQuery query);

    /** @return the last messages, oldest first; empty when the conversation is not in the organization */
    List<Message> handle(GetRecentMessagesQuery query);
}
