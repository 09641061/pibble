package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

import java.util.Objects;

/**
 * W10: Meta reports what happened to a message the line sent.
 *
 * @param pricingCategory such as {@code service} or {@code utility}; null when Meta sent none
 * @param error Meta's error code and title when the message failed
 */
public record RecordDeliveryStatusCommand(Wamid wamid, DeliveryStatus status, String pricingCategory, Boolean billable,
                                          String error) implements WhatsappWebhookCommand {
    public RecordDeliveryStatusCommand {
        Objects.requireNonNull(wamid, "wamid is required");
        Objects.requireNonNull(status, "status is required");
    }
}
