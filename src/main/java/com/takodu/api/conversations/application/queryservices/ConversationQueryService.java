package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.queries.GetConversationByIdQuery;
import com.takodu.api.conversations.domain.model.queries.GetConversationsByEstablishmentQuery;

import java.util.List;
import java.util.Optional;

/** W7: staff read the inbox of the establishments they see; owners and managers read every inbox. */
public interface ConversationQueryService {

    /**
     * @return one page, newest activity first
     * @throws com.takodu.api.conversations.domain.exceptions.EstablishmentNotFoundException when the establishment is
     *         not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when staff are not assigned to it
     */
    List<InboxConversation> handle(GetConversationsByEstablishmentQuery query);

    /**
     * @return empty when the conversation is not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when staff are not assigned to its
     *         establishment
     */
    Optional<InboxConversation> handle(GetConversationByIdQuery query);
}
