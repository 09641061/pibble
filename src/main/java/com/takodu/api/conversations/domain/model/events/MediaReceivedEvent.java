package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.MessageId;

/** A stored message carries media to download; after commit it wakes the media job instead of waiting for its poll. */
public record MediaReceivedEvent(MessageId messageId) { }
