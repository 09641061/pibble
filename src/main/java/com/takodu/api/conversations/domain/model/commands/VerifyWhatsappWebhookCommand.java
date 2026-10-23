package com.takodu.api.conversations.domain.model.commands;

/** Meta's handshake when the webhook URL is configured: {@code hub.mode}, {@code hub.verify_token}, {@code hub.challenge}. */
public record VerifyWhatsappWebhookCommand(String mode, String verifyToken, String challenge) { }
