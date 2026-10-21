package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;

import java.time.Instant;

/** Every writer locks the conversation first, so a webhook and a staff reply never lose each other's changes. */
public interface ConversationRepository {

    /**
     * W3: finds the line's conversation with the customer, starting it if there is none, and locks it until the
     * transaction ends. Safe when two deliveries start the same conversation at once.
     *
     * @param firstMessageAt when the message that starts it was sent, so imported history keeps its place in the inbox
     */
    Conversation lockOrStart(WhatsappLine line, CustomerId customerId, Instant firstMessageAt);

    Conversation save(Conversation conversation);
}
