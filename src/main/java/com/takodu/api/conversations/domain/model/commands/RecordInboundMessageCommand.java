package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

import java.time.Instant;
import java.util.Objects;

/**
 * A customer wrote to the line.
 *
 * @param phoneNumberId the line's Meta id
 * @param customerWaId the sender's WhatsApp id, the number without {@code +}
 * @param profileName the sender's WhatsApp profile name, optional
 */
public record RecordInboundMessageCommand(String phoneNumberId, String customerWaId, String profileName, Wamid wamid,
                                          MessageContent content, Instant occurredAt) implements WhatsappWebhookCommand {
    public RecordInboundMessageCommand {
        Objects.requireNonNull(phoneNumberId, "phoneNumberId is required");
        Objects.requireNonNull(customerWaId, "customerWaId is required");
        Objects.requireNonNull(wamid, "wamid is required");
        Objects.requireNonNull(content, "content is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
