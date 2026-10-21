package com.takodu.api.conversations.domain.model.commands;

import java.util.Objects;

/**
 * Meta reports that the business removed Takodu or was disconnected, banned or offboarded: the account's lines stop.
 *
 * @param wabaId the WhatsApp Business Account Meta reported about
 */
public record DisconnectAccountLinesCommand(String wabaId) implements WhatsappWebhookCommand {
    public DisconnectAccountLinesCommand {
        Objects.requireNonNull(wabaId, "wabaId is required");
    }
}
