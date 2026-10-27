package com.takodu.api.conversations.domain.model.valueobjects;

/**
 * The chats of one establishment that need someone.
 *
 * @param awaitingReply chats where {@code Conversation.awaitsReply()} holds
 * @param unread chats with at least one unread message (W8)
 */
public record InboxSummary(int awaitingReply, int unread) {
    public InboxSummary {
        if (awaitingReply < 0 || unread < 0) throw new IllegalArgumentException("counts must not be negative");
    }
}
