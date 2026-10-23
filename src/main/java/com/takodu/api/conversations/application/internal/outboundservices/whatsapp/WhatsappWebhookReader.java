package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.commands.WhatsappWebhookCommand;

import java.util.List;

/** Reads Meta's webhooks. Only its implementation knows Meta's JSON and signature scheme. */
public interface WhatsappWebhookReader {

    /** @return whether the body was signed with Takodu's app secret; checked on the raw bytes before parsing */
    boolean isAuthentic(byte[] body, String signature);

    /** @return whether the handshake's token is the configured verify token */
    boolean acceptsVerifyToken(String verifyToken);

    /**
     * @return the facts the delivery reports, in order; parts Takodu does not use or cannot read are skipped
     * @throws IllegalArgumentException when the body is not a JSON webhook
     */
    List<WhatsappWebhookCommand> read(byte[] body);
}
