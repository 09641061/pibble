package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.MessageId;

/** A message waits in the outbox; after commit it wakes the sender instead of waiting for its next poll. */
public record OutboundMessageQueuedEvent(MessageId messageId) { }
