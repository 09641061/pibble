package com.takodu.api.conversations.interfaces.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the assistant may ask and do in conversations. No method checks a caller: turns have none. */
public interface ConversationsContextFacade {
    /** @return the chat's state for A2, empty when not in the organization */
    Optional<AssistantConversationView> fetchConversation(UUID organizationId, UUID conversationId);

    /**
     * @param limit at most 100
     * @return the last messages, oldest first
     */
    List<ChatLineView> fetchRecentMessages(UUID organizationId, UUID conversationId, int limit);

    /**
     * A8: queues the assistant's reply, as text or with buttons or a list. It is dropped when the assistant may no longer answer (A2): a person
     * wrote meanwhile, it was handed off, the window closed, the line disconnected or the plan lapsed.
     *
     * @param choices the buttons or list under the text; null for a text reply
     * @param idempotencyKey the same for every attempt of one turn, so the reply is queued once
     * @return whether the reply is queued
     * @throws IllegalArgumentException when WhatsApp would refuse the choices
     */
    boolean sendAssistantReply(UUID organizationId, UUID conversationId, String text, ReplyChoicesView choices,
                               UUID idempotencyKey);

    /** A7: pauses the assistant until staff resume it and leaves the chat unread with the summary. */
    void handOff(UUID organizationId, UUID conversationId, String summary);

    /** Shows the open inboxes that the assistant is answering the chat ({@code typing}) or stopped; changes nothing. */
    void reportAssistantActivity(UUID organizationId, UUID conversationId, boolean typing);
}
