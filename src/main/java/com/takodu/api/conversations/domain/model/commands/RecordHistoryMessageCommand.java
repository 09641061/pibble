package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.MessageContent;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

import java.time.Instant;
import java.util.Objects;

/**
 * One message of the chat history the owner shared when connecting in coexistence mode.
 *
 * @param customerWaId the customer of the thread
 * @param fromCustomer whether the customer wrote it, rather than the business
 * @param status the last status of a message the business sent
 */
public record RecordHistoryMessageCommand(String phoneNumberId, String customerWaId, boolean fromCustomer, Wamid wamid,
                                          MessageContent content, DeliveryStatus status, Instant occurredAt)
        implements WhatsappWebhookCommand {
    public RecordHistoryMessageCommand {
        Objects.requireNonNull(phoneNumberId, "phoneNumberId is required");
        Objects.requireNonNull(customerWaId, "customerWaId is required");
        Objects.requireNonNull(wamid, "wamid is required");
        Objects.requireNonNull(content, "content is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
