package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

import java.time.Instant;
import java.util.Objects;

/**
 * W5: the owner wrote to a customer from the WhatsApp Business app (a coexistence echo).
 *
 * @param customerWaId the recipient's WhatsApp id
 */
public record RecordBusinessAppEchoCommand(String phoneNumberId, String customerWaId, Wamid wamid,
                                           MessageContent content, Instant occurredAt) implements WhatsappWebhookCommand {
    public RecordBusinessAppEchoCommand {
        Objects.requireNonNull(phoneNumberId, "phoneNumberId is required");
        Objects.requireNonNull(customerWaId, "customerWaId is required");
        Objects.requireNonNull(wamid, "wamid is required");
        Objects.requireNonNull(content, "content is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
