package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.InboxSummary;
import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Every writer locks the conversation first, so a webhook and a staff reply never lose each other's changes. */
public interface ConversationRepository {

    /**
     * W3: finds the line's conversation with the customer, starting it if there is none, and locks it until the
     * transaction ends. Safe when two deliveries start the same conversation at once.
     *
     * @param firstMessageAt when the message that starts it was sent, so imported history keeps its place in the inbox
     */
    Conversation lockOrStart(WhatsappLine line, CustomerId customerId, Instant firstMessageAt);

    Optional<Conversation> findById(OrganizationId organizationId, ConversationId conversationId);

    /** Locks the conversation until the transaction ends, like {@link #lockOrStart}. */
    Optional<Conversation> lockById(OrganizationId organizationId, ConversationId conversationId);

    /** @return the establishment's conversations, newest activity first, after the cursor */
    List<Conversation> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId,
                                           PageCursor after, int limit);

    Conversation save(Conversation conversation);

    /** @param establishmentId an establishment the caller checked belongs to the organization */
    InboxSummary summarize(OrganizationId organizationId, EstablishmentId establishmentId);
}
