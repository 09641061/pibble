package com.takodu.api.assistant.domain.model.commands;

import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;

/**
 * A customer message was stored; the assistant answers it later if A2 holds now.
 *
 * @param kind the message kind in lower case, such as {@code text} or {@code audio}
 * @param sentAt when the customer wrote
 */
public record EnqueueAssistantTurnCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                          ConversationId conversationId, CustomerId customerId, MessageId messageId,
                                          String kind, Instant sentAt) {
    public EnqueueAssistantTurnCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (conversationId == null) throw new IllegalArgumentException("conversationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (messageId == null) throw new IllegalArgumentException("messageId is required");
        if (kind == null) throw new IllegalArgumentException("kind is required");
        if (sentAt == null) throw new IllegalArgumentException("sentAt is required");
    }
}
