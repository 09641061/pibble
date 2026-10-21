package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;
import com.takodu.api.conversations.domain.model.valueobjects.CustomerContact;

/**
 * A conversation as the inbox shows it.
 *
 * @param customer null when customers no longer knows the customer
 * @param windowOpen whether free-form messages may be sent now (W6)
 * @param assistantPaused whether the assistant stays quiet now (A3)
 */
public record InboxConversation(Conversation conversation, CustomerContact customer, boolean windowOpen,
                                boolean assistantPaused) { }
