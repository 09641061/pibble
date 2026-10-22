package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.exceptions.WhatsappSignupFailedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;

/**
 * W2: the Embedded Signup steps after the owner's browser returns. The owner waits for the result, so the command
 * service calls it during the request, before the transaction that stores the line (takodu-jobs).
 */
public interface WhatsappOnboarding {

    /**
     * Exchanges the code for the business token, subscribes Takodu to the account's webhooks, reads the number,
     * requests the chat history (coexistence) or registers the number with the PIN (cloud API), and creates the
     * notification templates the account does not have yet.
     *
     * @throws WhatsappSignupFailedException when Meta refuses a step
     */
    OnboardedNumber onboard(String code, String wabaId, String phoneNumberId, LineMode mode, String pin);

    /** Stops the account's webhooks after the owner disconnects. Best effort: the webhook ignores the line anyway. */
    void unsubscribe(WhatsappLine line);
}
