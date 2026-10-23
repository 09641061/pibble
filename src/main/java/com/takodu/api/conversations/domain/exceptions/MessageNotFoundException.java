package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

public class MessageNotFoundException extends ResourceNotFoundException {
    public MessageNotFoundException(MessageId messageId) {
        super("Message", messageId.value());
    }
}
