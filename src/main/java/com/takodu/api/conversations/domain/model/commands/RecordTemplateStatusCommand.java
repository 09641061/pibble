package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.TemplateStatus;

import java.util.Objects;

/**
 * Meta reviewed, paused or disabled one of Takodu's templates on a WhatsApp Business Account.
 *
 * @param wabaId the account the template belongs to; every line of the account shares it
 */
public record RecordTemplateStatusCommand(String wabaId, NotificationTemplate template, TemplateStatus status)
        implements WhatsappWebhookCommand {
    public RecordTemplateStatusCommand {
        Objects.requireNonNull(wabaId, "wabaId is required");
        Objects.requireNonNull(template, "template is required");
        Objects.requireNonNull(status, "status is required");
    }
}
