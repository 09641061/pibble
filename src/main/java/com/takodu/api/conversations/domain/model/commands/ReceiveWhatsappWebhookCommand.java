package com.takodu.api.conversations.domain.model.commands;

/**
 * A delivery to {@code POST /webhooks/whatsapp}, before anything is trusted.
 *
 * @param body the raw bytes, which the signature covers
 * @param signature the {@code X-Hub-Signature-256} header, such as {@code sha256=…}; null when missing
 */
public record ReceiveWhatsappWebhookCommand(byte[] body, String signature) {
    public ReceiveWhatsappWebhookCommand {
        if (body == null) body = new byte[0];
    }
}
