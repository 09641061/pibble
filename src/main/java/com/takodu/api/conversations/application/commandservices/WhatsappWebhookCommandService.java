package com.takodu.api.conversations.application.commandservices;

import com.takodu.api.conversations.domain.exceptions.InvalidWebhookSignatureException;
import com.takodu.api.conversations.domain.exceptions.WebhookVerificationFailedException;
import com.takodu.api.conversations.domain.model.commands.ReceiveWhatsappWebhookCommand;
import com.takodu.api.conversations.domain.model.commands.VerifyWhatsappWebhookCommand;

/**
 * Stores what Meta reports, in one transaction, before the webhook answers; it calls nothing outside the database.
 * Webhooks have no {@code TenantContext}: each fact finds its organization through the line or the message.
 */
public interface WhatsappWebhookCommandService {

    /**
     * W4, W5, W10: stores customer messages, echoes from the WhatsApp Business app and history once per wamid,
     * moves delivery statuses forward, and disconnects lines Meta reports removed. Facts for an unknown or
     * disconnected line are ignored.
     *
     * @throws InvalidWebhookSignatureException when the signature is missing or wrong; nothing is stored
     * @throws IllegalArgumentException when the signed body is not a webhook
     */
    void handle(ReceiveWhatsappWebhookCommand command);

    /**
     * @return the challenge, which Meta expects echoed
     * @throws WebhookVerificationFailedException when the mode or the verify token is wrong
     */
    String handle(VerifyWhatsappWebhookCommand command);
}
