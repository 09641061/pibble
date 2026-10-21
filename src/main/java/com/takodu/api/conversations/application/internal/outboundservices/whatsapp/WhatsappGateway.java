package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

/** Sends through Meta's Cloud API with the line's business token. Only jobs call it, never a request or webhook. */
public interface WhatsappGateway {

    /**
     * @param to the customer's WhatsApp id, the number without {@code +}
     * @return the id Meta gave the message, which status webhooks carry
     * @throws WhatsappSendException when Meta refuses or does not answer
     */
    Wamid sendText(WhatsappLine line, String to, String body);
}
