package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

public class ConversationNotFoundException extends ResourceNotFoundException {
    public ConversationNotFoundException(ConversationId conversationId) {
        super("Conversation", conversationId.value());
    }
}
