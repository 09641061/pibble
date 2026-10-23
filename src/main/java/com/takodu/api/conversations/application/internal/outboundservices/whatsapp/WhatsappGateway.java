package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;

import java.util.List;

/** Sends through Meta's Cloud API with the line's business token. Only jobs call it, never a request or webhook. */
public interface WhatsappGateway {

    /**
     * @param to the customer's WhatsApp id, the number without {@code +}
     * @return the id Meta gave the message, which status webhooks carry
     * @throws WhatsappSendException when Meta refuses or does not answer
     */
    Wamid sendText(WhatsappLine line, String to, String body);

    /**
     * Sends reply buttons; a tap comes back as an inbound {@code interactive} message with the title. Window open only.
     *
     * @param titles 1 to 3, each at most 20 characters and different
     * @return the id Meta gave the message
     * @throws WhatsappSendException when Meta refuses or does not answer
     */
    Wamid sendButtons(WhatsappLine line, String to, String body, List<String> titles);

    /**
     * Sends a list the customer opens with {@code buttonLabel}; a tap comes back like a button's. Window open only.
     *
     * @param rows 1 to 10 titles, each at most 24 characters
     * @return the id Meta gave the message
     * @throws WhatsappSendException when Meta refuses or does not answer
     */
    Wamid sendList(WhatsappLine line, String to, String body, String buttonLabel, List<String> rows);

    /**
     * Sends an approved template, which WhatsApp delivers even when the window is closed (W6).
     *
     * @param templateName such as {@code takodu_reminder}, in {@code language}
     * @param parameters the body's parameters, in order
     * @return the id Meta gave the message
     * @throws WhatsappSendException when Meta refuses or does not answer
     */
    Wamid sendTemplate(WhatsappLine line, String to, String templateName, String language, List<String> parameters);
}
