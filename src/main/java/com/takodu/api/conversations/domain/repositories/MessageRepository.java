package com.takodu.api.conversations.domain.repositories;

import com.takodu.api.conversations.domain.model.aggregates.Message;
import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    @CrossTenant("The webhook skips a wamid it already stored (W4); wamids are unique across WhatsApp")
    boolean existsByWamid(Wamid wamid);

    @CrossTenant("A status webhook updates the message it reports on; the message carries its organization")
    Optional<Message> lockByWamid(Wamid wamid);

    Optional<Message> findById(OrganizationId organizationId, MessageId messageId);

    Optional<Message> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /** @return the conversation's messages, newest first, before the cursor */
    List<Message> findByConversation(OrganizationId organizationId, ConversationId conversationId, PageCursor before,
                                     int limit);

    /**
     * Locks the oldest outbound message due by {@code now} with {@code FOR UPDATE SKIP LOCKED}, so two senders never
     * take the same one.
     */
    @CrossTenant("The sender job claims due messages of every organization; the message carries its organization")
    Optional<Message> claimNextPending(Instant now);

    /**
     * Locks the message whose media download is due first by {@code now} with {@code FOR UPDATE SKIP LOCKED}, so two
     * media jobs never download the same file.
     */
    @CrossTenant("The media job claims due downloads of every organization; the message carries its organization")
    Optional<Message> claimNextMediaDue(Instant now);

    /**
     * Locks the message whose media goes first with {@code FOR UPDATE SKIP LOCKED}: media whose customer was deleted,
     * then media that arrived before {@code expiredBefore} and is not removed yet.
     */
    @CrossTenant("The retention job removes media of every organization; the message carries its organization")
    Optional<Message> claimNextMediaRemoval(Instant expiredBefore);

    /** @return the customer's messages, in every conversation, whose media is not removed or being removed */
    List<Message> findMediaToRemove(OrganizationId organizationId, CustomerId customerId);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Message save(Message message);
}
